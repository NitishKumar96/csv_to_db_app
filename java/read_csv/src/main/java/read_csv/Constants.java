package read_csv;

public final class Constants {
	// DB CONSTANTS
	public static final String DB_URL = "jdbc:mysql://localhost:3306/csv_database";
	public static final String DB_NAME = "";
	public static final String DB_USER = "root";
	public static final String DB_PASSWORD = "root";

	// THREAD CONSTANTS
	public static final String SAVE_COUNT = "SAVE_COUNT"; // key in thread return map, containing count of success save
															// count
	public static final Integer BATCH_LIMIT = 10;
	public static final Integer QUEUE_SIZE = 65536;

	// SQL STATEMENTS
	public static final String CREATE_TABLE_SQL = "CREATE TABLE %s (id INT NOT NULL AUTO_INCREMENT %s, PRIMARY KEY (id)); ";
	public static final String INSERT_TABLE_SQL = "INSERT INTO %s(%s) VALUE (%s); ";
	public static final String SELECT_LINES_COUNT_SQL ="SELECT COUNT(*) as 'count' FROM %s;";
	
	public static final String UPDATE_MASTER_SQL = "UPDATE master_csv SET status = 0 WHERE table_name = '%s' AND status = 1 ;";
	public static final String SELECT_MASTER_SQL = "SELECT lines_read,lines_saved  FROM master_csv WHERE table_name = '%s' AND status = 1 LIMIT 1;";
	public static final String INSERT_MASTER_SQL = "INSERT INTO master_csv (csv_name,table_name,lines_read,lines_saved,time_taken,num_thread) VALUE ('%s','%s',%s,%s,%s,%s) ;";

	public static final String UPDATE_ANALYSIS_SQL = "UPDATE csv_analysis SET status = 0 WHERE table_name = '%s' AND status = 1 ;";
	public static final String INSERT_ANALYSIS_SQL = "INSERT INTO csv_analysis (table_name,keyword,operation,value) VALUE (?,?,?,?) ;";
	public static final String SELECT_ANALYSIS_SQL = "SELECT keyword,operation,value FROM csv_analysis WHERE status = 1 AND  table_name = '%s';";

	public static final String DATETIME_REGEX = "(\\d{4})-(\\d{2})-(\\d{2}) (\\d{2}):(\\d{2}):(\\d{2})";
	public static final String INTEGER_REGEX = "/^\\-?\\d*\\.?\\d*$/";
	public static final String VARCHAR_TYPE = "VARCHAR(250)";
	public static final String INT_TYPE = "INT";
	public static final String BIGINT_TYPE = "BIGINT";
	public static final String DATETIME_TYPE = "DATETIME";
}
