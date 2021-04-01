import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;
import java.util.logging.FileHandler;
import java.util.logging.SimpleFormatter;
import java.time.Instant;

/* Sample code for basic Server */

public class Server {

	public String timestamp_log(String s) {
		Instant instant = Instant.now();
		long timestampMilli = instant.toEpochMilli();
		return (s + "\t" + String.valueOf(timestampMilli) + "\n");
	}

	public static void main ( String args[] ) throws Exception {
		//Filehandler setup; Based on SO link: https://tinyurl.com/wnr73bnh
		String logname = String.join("-", args);
		private static Logger logger = Logger.getLogger(logname);
		FileHandler fh;

		try {
			// This block configure the logger with handler and formatter
			fh = new FileHandler("../logs/" + logname);
			logger.addHandler(fh);
			SimpleFormatter formatter = new SimpleFormatter();
			fh.setFormatter(formatter);

			logger.info("----------------Init logs---------------\n");

		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}


		if (args.length != 3) throw new Exception("Need 3 args: <cloud_ip> <cloud_port> <VM id>");
		// convert strings
		public int ipaddy;
		public int port;
		public int id;
		try {
			ipaddy = Integer.parseInt(args[0]);
			port = Integer.parseInt(args[1]);
			id = Integer.parseInt(args[2]);
		} catch (NumberFormatException e) {
			logger.info(timestamp_log("IP address: " + args[0]));
			logger.info(timestamp_log("Port: " + args[1]));
			logger.info(timestamp_log("ID: " + args[2]));
			e.printStackTrace();
		}

		ServerLib SL = new ServerLib( args[0], port );
		
		// register with load balancer so requests are sent to this server
		SL.register_frontend();
		// how do i know how many to start?
		int newid = Sl.startVM();

		// main loop
		while (true) {
			Cloud.FrontEndOps.Request r = SL.getNextRequest();
			SL.processRequest( r );
		}
	}
}

