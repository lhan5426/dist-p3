import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;
import java.util.logging.FileHandler;
import java.util.logging.SimpleFormatter;
import java.time.Instant;


/* Sample code for basic Server */

public class Server {

	public static int[] hardcoded = new int[]{1800};

	public static String timestamp_log(String s) throws Exception {
		try {
			Instant instant = Instant.now();
			long timestampMilli = instant.toEpochMilli();
			return (s + "\t" + String.valueOf(timestampMilli) + "\n");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "Failed to convert";
	}

	public static void main ( String args[] ) throws Exception {
		//Filehandler setup; Based on SO link: https://tinyurl.com/wnr73bnh
		/*
		String logname = String.join("-", args);
		Logger logger = Logger.getLogger(logname);
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
		 */


		if (args.length != 3) throw new Exception("Need 3 args: <cloud_ip> <cloud_port> <VM id>");
		// convert strings
		int ipaddy = -1;
		int port = -1;
		int id = -1;
		try {
			ipaddy = Integer.parseInt(args[0]);
			port = Integer.parseInt(args[1]);
			id = Integer.parseInt(args[2]);
		} catch (NumberFormatException e) {
			/*
			logger.info(timestamp_log("IP address: " + args[0]));
			logger.info(timestamp_log("Port: " + args[1]));
			logger.info(timestamp_log("ID: " + args[2]));
			 */
			e.printStackTrace();
		}

		ServerLib SL = new ServerLib( args[0], port );
		SL.getTime();

		//database VM - not sure if anything is needed here
		if (id == 0) {

		}

		// odd VM -  we will use as front tier server
		if (id % 2 != 0) {
			SL.register_frontend();
			SL.startVM();
		}

		// even VM - we will use as middle tier
		if (id % 2 != 0 && id != 0) {
			Cloud.FrontEndOps.Request r = SL.getNextRequest();
			SL.processRequest(r);
		}

		/*
		// register with load balancer so requests are sent to this server
		SL.register_frontend();
		// how do i know how many to start?
		for (int i = 0; i < 30; i++) {
			int newid = SL.startVM();
			//SL.doFrontEndWork(newid);
			Cloud.FrontEndOps.Request r = SL.getNextRequest();
			SL.processRequest(r);
		}

		 */

		// main loop
		/*
		while (true) {
			Cloud.FrontEndOps.Request r = SL.getNextRequest();
			SL.processRequest(r);
		}
		*/
	}
}

