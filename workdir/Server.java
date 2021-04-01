import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;
import java.util.logging.FileHandler;
import java.util.logging.SimpleFormatter;
import java.time.Instant;


/* Sample code for basic Server */

public class Server {

	public static int[] hardcoded = new int[]{
		3,3,3,3,
		3,3,7,8,
		10,10,10,10,
		10,10,10,7,
			// 10 may be okay for hour 19
		10,10,10,14,
			//try 10 to see if less timeouts
			// 13 12 10 10 originally
			// 1: failed 5 timeout 1 - 11 orig
			// 2: failed 1 timeout 6 - 12 orig

			// exp has 3 5 for 22, may want to bump up to 11
			// last one is 2 2 for normal
		14,11,11,10
	};

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
			fh = new FileHandler("/afs/andrew.cmu.edu/usr2/lawrench/private/440/15440-p3/workdir/logs/" + logname);
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
/*
		logger.info("IP: " + args[0]);
		logger.info("Port: " + args[1]);
		logger.info("ID: " + args[2]);

		logger.info("Stored Port: " + String.valueOf(port));
		logger.info("ID: " + String.valueOf(id));
*/		int num_inst = 2;
		ServerLib SL = new ServerLib( args[0], port );

		//logger.info("val: " + String.valueOf(hardcoded[(int) SL.getTime()]));
		//logger.info("ind: " + String.valueOf((int) SL.getTime()));

		//database VM - not sure if anything is needed here
		if (id == 0) {

		}
		//designated master node, started required # of VM;s
		if (id == 1) {
			//or (int i = 0; i < temp; i ++) {
			for (int i = 0; i < hardcoded[(int) SL.getTime()]; i ++) {
				SL.startVM();
			}
			int qextras = SL.getQueueLength();
			while (qextras > 2) {
				SL.dropHead();
				qextras -= 1;
			}
		}

		// odd VM -  we will use as front tier server + middle combined for now

		while (true) {
			SL.register_frontend();
			Cloud.FrontEndOps.Request r = SL.getNextRequest();
			SL.unregister_frontend();
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

