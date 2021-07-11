package read_csv;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.*;
import java.util.HashMap;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;
import org.apache.commons.csv.CSVFormat;

import static read_csv.Constants.*;

@Command(name = "csv_reader", mixinStandardHelpOptions = true, description = "Reads csv file and store it into mysql table.", version = "0.1")
public class ReadCsv implements Callable<Integer> {

	@Option(names = { "-f", "--file_name" }, description = ".csv file name we have read.")
	String file_name = "";
	@Option(names = { "-t", "--table_name" }, description = "table name, if not provided will make new table")
	String table_name = "";
	@Option(names = { "--thread_count" }, description = "number of threads to write data into db.")
	Integer thread_count = 1;
	@Option(names = { "-ai", "--analysis_index" }, description = "number of threads to write data into db.")
	Integer analysis_index = -1; // -1 no index to process
	@Option(names = {  "--batch_size" }, description = "batch size of DB insert")
	Integer batch_size = BATCH_LIMIT; 

	private Boolean new_table = false;
	protected AtomicBoolean is_done = new AtomicBoolean();
	protected int lines_read = 0;
	protected String[] col_names = null;
	protected String[] col_types = null;
	protected String col_name_sql = "";
	protected int col_count = 0;
	protected long time_taken =0;

	private Connection connection = null;

	@SuppressWarnings("resource")
	public Integer call() {
		if (file_name.length() > 0) {
			System.out.println("File name recieved: " + file_name);
		} else {
			System.out.println("Invalid file name provided.");
			return 1;
		}
		if (table_name.length() > 0) {
			System.out.println(table_name);
		} else {
			new_table = true;
		}

		// read csv file, read first 2 line to get column names and type
		BufferedReader line_reader = null;
		String current_line = null;
		
		// READ FIRST 2 LINES OF CSV
		try {
			line_reader = new BufferedReader(new FileReader(this.file_name));
			current_line = line_reader.readLine();
			this.col_names = current_line.toLowerCase().split(",");
			current_line = line_reader.readLine();
			this.col_types = current_line.split(",");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.out.println("Error while reading the csv file.\n");
			return 1; // non zero return
		} catch (IOException e) {
			System.out.println("Error while reading the csv file.\n");
			e.printStackTrace();
			return 1;
		}
 
		// PROCESS COLUMN NAMES AND 
		this.col_count = this.col_names.length;
		String value_list = "";
		for (int i = 0; i < this.col_count; i++) {
			String col_name = this.col_names[i].trim().replaceAll("[()?:!.,;{}%]+", "_");
			
			if(col_name.equals("")) col_name = "blank_"+i; 
			else if(col_name.equals("id")) col_name = col_name+"_"+i; 

			this.col_names[i]=col_name;
			this.col_types[i] = get_col_type(this.col_types[i]);
			if (this.col_name_sql.length() <= 1) {
				this.col_name_sql = this.col_names[i];
				value_list = "?";
			} else {
				this.col_name_sql = this.col_name_sql + ", " + this.col_names[i];
				value_list = value_list + ",?";
			}
		}
		CSVFormat formater = CSVFormat.DEFAULT
									.withTrim()
									.withAllowMissingColumnNames()
									.withHeader(this.col_names)
									;

		// make connection object
		try {
			Class.forName("com.mysql.cj.jdbc.Driver"); 
			this.connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
			this.connection.setAutoCommit(false);
		} catch (SQLException e) {
			System.out.println("Error while connecting to dastabase.");
			e.printStackTrace();
			return 1;
		} catch (ClassNotFoundException e) {
			System.out.println("Error dastabase class not found.");
			e.printStackTrace();
			return 1;
		}

		HashMap<String, Integer> thread_hash = null;
		if (this.new_table == true) {
			try {
				this.table_name = make_new_table();
			} catch (SQLException e) {
				System.out.println("Error while creating new table:\n");
				e.printStackTrace();
				return 1;
			}
		} else {
			thread_hash = read_old_analysis();
			this.lines_read = get_old_lines_read();
		}

		// PREPAIR DATA FOR THREAD
		String thread_insert_sql = String.format(INSERT_TABLE_SQL, this.table_name, this.col_name_sql, value_list);
		// make thread
		FutureTask<HashMap<String, Integer>>[] thread_list = new FutureTask[this.thread_count];
		BlockingQueue<String>[] master_queue = new PriorityBlockingQueue[this.thread_count];
		// PriorityBlockingQueue<String> master_queue = new PriorityBlockingQueue<String>(this.thread_count);
		this.is_done.set(false);

		for (int i = 0; i < this.thread_count; i++) {
			// have to initialise the queue also
			master_queue[i] = new PriorityBlockingQueue<String>(QUEUE_SIZE);

			Callable<HashMap<String, Integer>> callable = new ProcessThread(master_queue[i], this.is_done,
					thread_insert_sql, this.col_types, this.col_count, this.analysis_index, formater, this.batch_size,thread_hash);

			// Create the FutureTask with Callable
			thread_list[i] = new FutureTask<HashMap<String, Integer>>(callable);

			Thread t = new Thread(thread_list[i]);
			t.start();
		}

		// READ THE CSV AND ADD DATA INTO THE THREAD
		long start_time = System.nanoTime(); 
		int current_line_index = 0;
		try {
			int current_thread = 0;
			Boolean line_added = false;
			do {
				current_line_index++;
				line_added=false;
				// if the current line is new line
				if (current_line_index > this.lines_read) {
					while(line_added == false){
						// master_queue.add(current_line);
						if(master_queue[current_thread].size()< QUEUE_SIZE){
							master_queue[current_thread].put(current_line);
							line_added=true;
						}
						if (current_line_index % (this.batch_size * this.thread_count) == 0 & current_line_index > this.lines_read) {
							this.lines_read = current_line_index;
							update_master_count(this.lines_read, 0, System.nanoTime()- start_time, this.thread_count);
						}
						current_thread++;
						if (current_thread >= this.thread_count) {
							current_thread = current_thread % this.thread_count;
						}
					}
				}

				
			} while ((current_line = line_reader.readLine()) != null);

			line_reader.close();

		} catch (IOException e) {
			System.out.println("Error while reading csv file:\n");
			e.printStackTrace();
		} catch (InterruptedException e) {
			System.out.println("Error while addign line to thread queue:\n");
			e.printStackTrace();
		} catch (SQLException e) {
			System.out.println("Error while updating database:\n");
			e.printStackTrace();
		}

		// close all threads
		this.is_done.set(true); // update flag so that thread can save there value and exit
		Boolean thread_running = true;
		while (thread_running) { // wait while other threads are running
			for (int i = 0; i < this.thread_count; i++) {
				if (thread_list[i].isDone() == true) {
					thread_running = false;
				} else {
					thread_running = true;
				}
			}
		}
		this.time_taken = System.nanoTime()- start_time;
		HashMap<String, Integer> final_hash = new HashMap<String, Integer>();
		final_hash.put("READ_COUNT", current_line_index);
		for (int i = 0; i < this.thread_count; i++) {
			try {
				// reuse thread hash
				thread_hash = thread_list[i].get();
				final_hash = merge_hash(final_hash, thread_hash);
			} catch (InterruptedException e) {
				e.printStackTrace();
				continue;
			} catch (ExecutionException e) {
				e.printStackTrace();
				continue;
			}
		}

		// save final values
		try {
			save_final_value(final_hash);
		} catch (SQLException e) {
			System.out.println("Error while saving final results.\n");
			e.printStackTrace();
			return 1;
		}

		return 0;
	}

	private void save_final_value(HashMap<String, Integer> final_hash) throws SQLException {
		// UPDATE MASTER TABLE
		update_master_count(final_hash.get("READ_COUNT"), final_hash.get(SAVE_COUNT),this.time_taken, this.thread_count);
		// UPDATE ANALYSIS TABLE
		String sql = String.format(UPDATE_ANALYSIS_SQL, this.table_name);
		Statement statement = this.connection.createStatement();
		statement.execute(sql);

		sql = INSERT_ANALYSIS_SQL;
		PreparedStatement analysis_statement = this.connection.prepareStatement(sql);
		for (String key : final_hash.keySet()) {
			if( key.equals("READ_COUNT")| key.equals(SAVE_COUNT)) continue;
			analysis_statement.setString(1, this.table_name);
			analysis_statement.setString(2, key);
			analysis_statement.setString(3, "COUNT");
			analysis_statement.setInt(4, final_hash.get(key));
			analysis_statement.addBatch();
		}
		// System.out.println(analysis_statement);
		analysis_statement.executeBatch();
		this.connection.commit();
	}

	private HashMap<String, Integer> merge_hash(HashMap<String, Integer> final_hash,
			HashMap<String, Integer> thread_hash) {
		Set<String> keys = thread_hash.keySet();
		for (String key : keys) {
			if (final_hash.containsKey(key)) {
				final_hash.put(key, final_hash.get(key) + thread_hash.get(key));
			} else {
				final_hash.put(key, thread_hash.get(key));
			}
		}

		return final_hash;
	}

	private void update_master_count(int current_line_index, int lines_saved, long time_taken, int num_thread) throws SQLException {
		// Deactivate old row
		String sql = String.format(UPDATE_MASTER_SQL, this.table_name);
		Statement statement = this.connection.createStatement();
		statement.execute(sql);
		// insert new row
		sql = String.format(INSERT_MASTER_SQL, this.file_name, this.table_name, current_line_index, lines_saved, time_taken, num_thread);
		statement.execute(sql);
		this.connection.commit();
		this.lines_read = current_line_index;

	}

	private String make_new_table() throws SQLException {
		UUID uuid = UUID.randomUUID();
		table_name = uuid.toString().replaceAll("[()?:!.,;{}%-]+", "");
		String col_sql = "";
		for (int i = 0; i < this.col_count; i++) {
			col_sql = col_sql + ", " + this.col_names[i] + " " + this.col_types[i];
		}

		String create_table_sql = String.format(CREATE_TABLE_SQL, this.table_name, col_sql);

		// for debug
		// System.out.println("Generated create table sql : \n" + create_table_sql);

		Statement statement = this.connection.createStatement();
		statement.execute(create_table_sql);
		this.connection.commit();
		return table_name;
	}

	private int get_old_lines_read() {
		int lines_read = 0;
		String sql = String.format(SELECT_LINES_COUNT_SQL, this.table_name);
		ResultSet sql_result = null;

		try {
			Statement statement = this.connection.createStatement();
			sql_result = statement.executeQuery(sql);

			while (sql_result.next()) {
				lines_read = sql_result.getInt("count");
			}

		} catch (Exception e) {
			System.out.println("Error while reading lines saved count:\n");
			e.printStackTrace();
			return 0;
		}

		return lines_read;
	}

	private HashMap<String, Integer> read_old_analysis() {
		HashMap<String, Integer> analysis = new HashMap<String, Integer>();

		String sql = String.format(SELECT_ANALYSIS_SQL, this.table_name);
		ResultSet sql_result = null;

		try {
			Statement statement = this.connection.createStatement();
			sql_result = statement.executeQuery(sql);

			// FILL HASHMAP
			while (sql_result.next()) {
				analysis.put(sql_result.getString("keyword"), sql_result.getInt("value"));
			}

		} catch (Exception e) {
			System.out.println("Error while reading lines saved count:\n");
			e.printStackTrace();
			return null;
		}

		return analysis;
	}

	private String get_col_type(String col_data) {
		if (Pattern.matches(INTEGER_REGEX, col_data)) {
			if (col_data.length() < 10)
				return INT_TYPE;
			else
				return BIGINT_TYPE;
		} else if (Pattern.matches(DATETIME_REGEX, col_data)) {
			return DATETIME_TYPE;
		} else { // for varchar type
			return VARCHAR_TYPE;
		}
	}

	public static void main(String... args) {
		int exitCode = new CommandLine(new ReadCsv()).execute(args);
		System.exit(exitCode);
	}

}

