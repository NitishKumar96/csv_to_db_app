package read_csv;

import java.math.BigDecimal;
import java.sql.*;
import java.util.HashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import static read_csv.Constants.*;

public class ProcessThread implements Callable<HashMap<String, Integer>> {

	protected BlockingQueue<?> queue = null;
	private AtomicBoolean is_done;
	protected String sql = "";
	protected String[] col_type = null;
	protected Integer col_count = 0;
	protected Integer count_index = -1;
	protected HashMap<String, Integer> final_result = new HashMap<String, Integer>();

	public ProcessThread(BlockingQueue<String> queue, AtomicBoolean is_done, String sql, String[] col_type,
			Integer col_count, Integer count_index, HashMap<String, Integer> final_result) {
		this.queue = queue;
		this.is_done = is_done;
		this.sql = sql;
		this.col_type = col_type;
		this.col_count = col_count;
		this.count_index = count_index;
		if (final_result != null) {
			this.final_result = final_result;
		}

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

				String data_string = (String) this.queue.take();
				String[] data = data_string.split(",");

				// loop through all the value in row
				for (int index = 0; index <= this.col_count; index++) {
					// add data into statement
					if (col_type[index] == INT_TYPE) {
						statement.setInt(index, Integer.parseInt(data[index]));
					} else if (col_type[index] == BIGINT_TYPE) {
						statement.setBigDecimal(index, new BigDecimal(data[index]));
					} else { // datetime and string will be handled by jdbc driver
						statement.setString(index, data[index]);
					}
					// update return hashmap
					if (index == count_index && count_index > -1) {
						// update/add key count in hashmap
						if (this.final_result.containsKey(data[index])) {
							this.final_result.put(data[index], this.final_result.get(data[index]) + 1);
						} else {
							final_result.put(data[index], 1);
						}
					}
				}
				statement.addBatch();
				batch_count++;
			}

			if (batch_count > 0) {
				if (batch_count % BATCH_LIMIT == 0 || this.is_done.get() == true) {
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

			if (this.is_done.get() == true && batch_count == 0) {
				running = false;
				break;
			}
		}
		// close all connections
		connection.commit();
		connection.close();

		return this.final_result;
	}

}
