import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;
import java.util.logging.FileHandler;
import java.util.logging.SimpleFormatter;
import java.time.Instant;
import java.rmi.Naming;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import java.util.concurrent.ArrayBlockingQueue;

public class Server {

	private static AppOps from_front = null;

	public static int[] hardcoded = new int[]{
		3,2,2,2,
		3,3,2,8,
		4,10,10,10,
		10,10,10,7,
			// 10 may be okay for hour 19
		10,10,10,5,
			//try 10 to see if less timeouts
			// 13 12 10 10 originally
			// 1: failed 5 timeout 1 - 11 orig
			// 2: failed 1 timeout 6 - 12 orig

			// exp has 3 5 for 22, may want to bump up to 11
			// last one is 2 2 for normal
		14,11,11,10
	};

	public static int[] bardcoded = new int[]{
			1,1,1,1,
			3,3,2,8,
			4,10,10,10,
			10,10,10,7,
			// 10 may be okay for hour 19
			10,10,10,5,
			//try 10 to see if less timeouts
			// 13 12 10 10 originally
			// 1: failed 5 timeout 1 - 11 orig
			// 2: failed 1 timeout 6 - 12 orig

			// exp has 3 5 for 22, may want to bump up to 11
			// last one is 2 2 for normal
			14,11,11,10
	};

	private static class AppOpsImpl extends UnicastRemoteObject implements Server.AppOps {
		private ArrayBlockingQueue<Cloud.FrontEndOps.Request> jobs = new ArrayBlockingQueue<Cloud.FrontEndOps.Request>(5);

		public AppOpsImpl() throws RemoteException {
			super(0);
		}

		public synchronized boolean queueRequest(Cloud.FrontEndOps.Request var1) {
			this.notifyAll();
			this.jobs.add(var1);
			return true;
		}

		public synchronized int getLength() {
			return this.jobs.size();
		}

		public synchronized Cloud.FrontEndOps.Request removeHead() {
			Cloud.FrontEndOps.Request r = null;

			try {
				r = (Cloud.FrontEndOps.Request)this.jobs.take();
			}
			catch (InterruptedException e1) {
				e1.printStackTrace();
			}
			return r;
		}

		public synchronized Cloud.FrontEndOps.Request getHead() {
			Cloud.FrontEndOps.Request r = null;
			r = (Cloud.FrontEndOps.Request)this.jobs.peek();
			return r;
		}
	}

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




		if (args.length != 3) throw new Exception("Need 3 args: <cloud_ip> <cloud_port> <VM id>");
		// convert strings
		Server.AppOps temp = null;
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

//		logger.info("IP: " + args[0]);
//		logger.info("Port: " + args[1]);
//		logger.info("ID: " + args[2]);
//
//		logger.info("Stored Port: " + String.valueOf(port));
//		logger.info("ID: " + String.valueOf(id));

		ServerLib SL = new ServerLib( args[0], port );
		int upscale_thres = 0;
		int downscale_thres = 0;
		// total includes master node so technically should never be below 1
		int curr_total = hardcoded[(int) SL.getTime()];
		int endind = curr_total;
//		logger.info("val: " + String.valueOf(hardcoded[(int) SL.getTime()]));
//		logger.info("ind: " + String.valueOf((int) SL.getTime()));

		//designated master node, started required # of VMs
		//can temporarily act as monolith and handle queued requests
		if (id == 1) {
			//or (int i = 0; i < temp; i ++) {

			for (int i = 0; i < curr_total; i ++) {
				SL.startVM();
			}

			//TODO: fix/include early dropout
			int qextras = SL.getQueueLength();
			/*
			while (qextras > 2) {
				SL.dropHead();
				qextras -= 1;
			}

			 */
			// While other VM's boot, we handle initial queued requests in master node
			// Cannot stay in req handling mode longer than 10 seconds according to Piazza
			// ALthough there is a big penalty for switching - ideally immediately
//			while (true) {
//				SL.register_frontend();
//				Cloud.FrontEndOps.Request r = SL.getNextRequest();
//				//last_times.add(SL.getTime());
//				/*
//				if (get_avg(last_times, endind) > 1) {
//					SL.startVM();
//				}
//
//				 */
//				SL.unregister_frontend();
//				SL.processRequest(r);
//			}
		}
		//TODO measure interarrival times;

		//qlength < 2 means server is keeping up, otherwise
		//this server is not keeping up
		//bool qlong = false;

		int num_front = ((int) Math.floor(bardcoded[(int) SL.getTime()]));

		//front end designated
		//must be at least 0 and 1 VM that are non front
		//so this should always be safe
		if (id > 1 && id < num_front+2) {
			logger.info("Inside some dogshit fronted: \n");
			// the size of Q needs to be fine tuned probably
			// TODO args[0] to ipaddy (may need to render args[0] a string)
			// TODO error handling for this casting
			// TODO RNSDNFOSDNF
			//  : i think you need to wait until some signal is sent from app server indicating it's up
			// this also needs to be outside the class and is prolly some serializable
			while (temp == null) {
				try {
					temp = (AppOps)Naming.lookup("//" + args[0] + ":" + port + "/AppOps");
					from_front = temp;
					SL.register_frontend();
					logger.info("Frontend registered\n");
					while (true) {
						Cloud.FrontEndOps.Request r = SL.getNextRequest();
						// if job is taking a long time and this put never happens
						// may need some form of timing benchmark to decide when to drop job
						from_front.queueRequest(r);
						Cloud.FrontEndOps.Request r0 = from_front.getHead();
						if (r0 != null) {
							logger.info(timestamp_log("From frontend pov, correctly stored req" + r0.toString() + "\n"));
						}
						//somehow do some RMI shit and send
						logger.info("Frontend should be queueing shit here\n");
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		//app server
		} else {
			logger.info("Inside some app server...\n");
			//Each backend needs to create its own threadsafe queue in addition to others
			//Receiving rolling average from each app server may also be good
			//Somehow get req from RMI
			AppOpsImpl to_mid = new AppOpsImpl();
			Naming.rebind("//localhost:" + port + "/AppOps", to_mid);

			while (true) {
				Cloud.FrontEndOps.Request r0 = to_mid.getHead();
				while (r0 == null) {
					//logger.info(timestamp_log("currently waiting for req to come in\n"));
				}
				logger.info("\tcurrent req: " + r0.toString() + "\n");
				Cloud.FrontEndOps.Request r = to_mid.removeHead();
				SL.processRequest(r);
				logger.info("\tcompleted req: " + r.toString() + "\n");
			}
		}

		/*
		while (true) {
			SL.register_frontend();
			Cloud.FrontEndOps.Request r = SL.getNextRequest();
			SL.unregister_frontend();
			SL.processRequest(r);
		}
		 */

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

	}

	//Interface for function provided to frontend to access backend
	public interface AppOps extends Remote {
		boolean queueRequest(Cloud.FrontEndOps.Request var1) throws RemoteException;
		int getLength() throws RemoteException;
		Cloud.FrontEndOps.Request removeHead() throws InterruptedException, RemoteException;
		Cloud.FrontEndOps.Request getHead() throws RemoteException;
	}

}

