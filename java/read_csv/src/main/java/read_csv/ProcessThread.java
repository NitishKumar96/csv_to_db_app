package read_csv;

import java.math.BigDecimal;
import java.sql.*;
import java.util.HashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
// import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord; 

import static read_csv.Constants.*;

public class ProcessThread implements Callable<HashMap<String, Integer>> {

	protected BlockingQueue<?> queue = null;
	private AtomicBoolean is_done;
	protected String sql = "";
	protected String[] col_type = null;
	protected Integer col_count = 0;
	protected Integer count_index = -1;
	protected HashMap<String, Integer> final_result = new HashMap<String, Integer>();
	protected CSVFormat csv_formater=null;
	protected int batch_size = BATCH_LIMIT;

	public ProcessThread(BlockingQueue<String> queue, AtomicBoolean is_done, String sql, String[] col_type,
			Integer col_count, Integer count_index, CSVFormat csv_formater, int batch_size,HashMap<String, Integer> final_result) {
		this.queue = queue;
		this.is_done = is_done;
		this.sql = sql;
		this.col_type = col_type;
		this.col_count = col_count;
		this.csv_formater = csv_formater;
		this.count_index = count_index;
		if (final_result != null) {
			this.final_result = final_result;
		}
		this.batch_size=batch_size;

	}

	public HashMap<String, Integer> call() throws Exception {
		Boolean running = true;
		Connection connection = null;
		connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
		connection.setAutoCommit(false);

		PreparedStatement statement = connection.prepareStatement(this.sql);
		
		int batch_count = 0;
		if (!final_result.containsKey(SAVE_COUNT))
			final_result.put(SAVE_COUNT, 0);// if save count is not present, add it
		while (running) {
			// if queue is not empty
			if (!this.queue.isEmpty()) {

				String data_string = (String) this.queue.poll();
				CSVRecord record = CSVParser.parse(data_string, this.csv_formater).getRecords().get(0);

				// String data_string = (String) this.queue.take();
				// String[] data = data_string.split(",");

				// loop through all the value in row
				for (int index = 0; index < this.col_count; index++) {
					// add data into statement
					String value = record.get(index);
					if (value.equals("")) value=null;
					if (col_type[index] == INT_TYPE) {
						statement.setInt(index+1, Integer.parseInt(value ));
					} else if (col_type[index] == BIGINT_TYPE) {
						statement.setBigDecimal(index+1, new BigDecimal(value ));
					} else { // datetime and string will be handled by jdbc driver
						statement.setString(index+1, value );
					}
					// update return hashmap
					if (this.count_index > -1){
						if (index == this.count_index) {
							// update/add key count in hashmap
							if (this.final_result.containsKey(record.get(index))) {
								this.final_result.put(record.get(index), this.final_result.get(record.get(index)) + 1);
							} else {
								final_result.put(record.get(index), 1);
							}
						}
					}
				}
				statement.addBatch();
				batch_count++;
				if (batch_count > 0) {
					if (batch_count % this.batch_size == 0 || this.is_done.get() == true) {
						try {
							statement.executeBatch();
							// increase save count
							final_result.put(SAVE_COUNT, final_result.get(SAVE_COUNT) + batch_count);
							batch_count = 0;
						} catch (SQLException ex) {
							ex.printStackTrace();
							try {
								connection.rollback();
							} catch (SQLException e) {
								e.printStackTrace();
							}
						}
					}
				}
			}else{
				if (this.is_done.get() == true ) {
					if ( batch_count == 0){
						running = false;
						break;
					}
					else{
						try {
							statement.executeBatch();
							// increase save count
							final_result.put(SAVE_COUNT, final_result.get(SAVE_COUNT) + batch_count);
							batch_count = 0;
							running = false;
							break;
						} catch (SQLException ex) {
							ex.printStackTrace();
							try {
								connection.rollback();
							} catch (SQLException e) {
								e.printStackTrace();
							}
						}
					}
					
				}
			}
			
		}
		// close all connections
		connection.commit();
		connection.close();

		return this.final_result;
	}

}
