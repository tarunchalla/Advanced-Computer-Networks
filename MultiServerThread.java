import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * 
 * @author Srinivas, Tarun
 *This class is used to maintain threads and connections to other hosts/routers.
 *and also does different operations based on different host/router commands received.
 */
public class MultiServerThread implements Runnable
{
	Socket socket;
	PrintWriter pwriter=null;
	BufferedReader in = null;
	String msgToServer=null;
	String routerId;
	HashMap<String, String> rpMap;
	public static LinkedHashSet<String> hostList= new LinkedHashSet<String>();
	public static HashMap<String, PrintWriter> hostPWMap = new HashMap<String, PrintWriter>();
	public static HashMap<String, PrintWriter> routerPWMap = new HashMap<String, PrintWriter>();

	MultiServerThread(Socket socket, String rId, HashMap<String, String> rpMap)
	{   
		System.out.println("Here in constructor");
		this.socket=socket;
		this.routerId = rId;
		this.rpMap = rpMap;
	}

	public void run()
	{

		try
		{
			System.out.println("here in multiserver threads");
			pwriter = new PrintWriter(socket.getOutputStream(), true);
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

			//System.out.println("Number of Clients: " +Router.writers.size());
			while(true)
			{
				try
				{
					msgToServer = in.readLine();
					System.out.println("Got the msgToServer:"+msgToServer);
					if(null != msgToServer){
						System.out.println("server got :"+msgToServer);
						String msgToServerArr[] = msgToServer.split(" ");
						String checkType = msgToServerArr[0].trim();

						String mgroupId;
						String hostId;
						String data;
						String frmRouter;
						String rendzPt;
						String srcId;
						if(checkType.equalsIgnoreCase("REPORT")
								&& msgToServerArr.length == 3){
							System.out.println("Inside REPORT message");
							hostId = msgToServerArr[1];
							mgroupId = msgToServerArr[2];
							hostPWMap = maintainPrintWriMap(hostPWMap, hostId,pwriter);
							doReportOperation(hostId,mgroupId);
						}
						else if(checkType.equalsIgnoreCase("LEAVE")
								&& msgToServerArr.length >= 3){
							hostId = msgToServerArr[1];
							mgroupId = msgToServerArr[2];
							System.out.println("leave check");
							doLeaveOperation(hostId,mgroupId);
						}
						else if(checkType.equalsIgnoreCase("LIST")){
						}
						else if(checkType.equalsIgnoreCase("SEND")
								&& msgToServerArr.length == 4){
							System.out.println("send check");
							hostId = msgToServerArr[1];
							mgroupId = msgToServerArr[2];
							data = msgToServerArr[3];
							doSendOperation(hostId,mgroupId,data);

						}
						else if(checkType.equalsIgnoreCase("JOIN")
								&& msgToServerArr.length >= 4){
							// JOIN from other router comes
							System.out.println("Received join msg from Router:"+msgToServer);
							frmRouter = msgToServerArr[1];
							rendzPt = msgToServerArr[2];
							mgroupId = msgToServerArr[3];
							doJoinOperation(frmRouter,rendzPt,mgroupId);
						}
						else if(checkType.equalsIgnoreCase("PRUNE")){
							frmRouter = msgToServerArr[1];
							rendzPt = msgToServerArr[2];
							mgroupId = msgToServerArr[3];

							doPruneOperation(frmRouter,rendzPt,mgroupId);
						}
						else if(checkType.equalsIgnoreCase("REGISTER")){
							//REGISTER FROM OTHER ROUTERS
							srcId = msgToServerArr[1];
							rendzPt = msgToServerArr[2];
							mgroupId = msgToServerArr[3];
							data = msgToServerArr[4];
							doRegisterOperation(srcId,rendzPt,mgroupId,data);
						}
						else if(checkType.equalsIgnoreCase("SSJOIN")){
							//SSJOIN FROM OTHER ROUTERS
							frmRouter = msgToServerArr[1];
							srcId = msgToServerArr[2];
							mgroupId = msgToServerArr[3];
							doSSJoinOperation(frmRouter,srcId,mgroupId);
						}
						else if(checkType.equalsIgnoreCase("MCAST")){
							//MCAST FROM OTHER ROUTERS
							frmRouter = msgToServerArr[1];
							srcId = msgToServerArr[2];
							mgroupId = msgToServerArr[3];
							data = msgToServerArr[4];
							doMulticastOperation(frmRouter,srcId,mgroupId,data);
						}
						else{
							System.out.println("wrong format");
						}
						System.out.println("Done the job--> waiting for any input now !!!!!!!!!!!!");
					}
				} 
				catch (IOException e)
				{
					System.out.println("yup it blew up here"+e);
				}
			}

		} catch(IOException e)
		{
			System.out.print("boom goes the dynamite"+ e);
		}
		finally{
			pwriter.close();
			try {
				in.close();
				socket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

	}
	/**
	 * This method sends the multicast message once it receives MCAST message from the router.
	 * @param frmRouter
	 * @param srcId
	 * @param mgroupId
	 * @param data
	 * @throws IOException
	 */
	private void doMulticastOperation(String frmRouter, String srcId, String mgroupId, String data) throws IOException {
		// TODO Auto-generated method stub
		System.out.println("---------------------------Inside doMulticastOperation-----------------------------");
		// dont send to router who send the mcast
		sendMCastMsg(srcId,mgroupId,data);
		System.out.println("---------------------------End of doMulticastOperation-----------------------------");
	}
	/**
	 * This method is used to send SSJOIN message to the source router once RP receives the REGISTER message.
	 * @param frmRouter
	 * @param srcId
	 * @param mgroupId
	 * @throws IOException
	 */
	private void doSSJoinOperation(String frmRouter, String srcId,String mgroupId) throws IOException {
		// TODO Auto-generated method stub
		System.out.println("---------------------------Inside doSSJoinOperation-----------------------------");
		updateForwardingSSJoinTable(frmRouter,srcId,mgroupId);
		String nextHopRouter;
		//check if it is a source id

		if(routerId.trim().equalsIgnoreCase(srcId)){
			// rendezpoint here
			System.out.println("Reached the SOURCE ROUTER BACK !!!!!!!! Reached the source Router");

			//dont know wat to do here
			//updateJoinForwardingTable(frmRouter,rendzPt,mgroupId);
		}
		else{
			// not rendezous point so calculate dijstras
			System.out.println("NOT A SOURCE POINT  SO CALCULATING DIJKSTRAS");
			System.out.println("here source:"+routerId+" trim:"+srcId);
			nextHopRouter = fetchNextHopUsingDijkstra(routerId,srcId);
			PrintWriter pwRouter = routerPWMap.get(nextHopRouter);
			String ssJoinMsg = String.format("SSJOIN %s %s %s", routerId,srcId,mgroupId);
			if(pwRouter != null){
				System.out.println("connection is already there--> so SSJOIn regMsg:"+ssJoinMsg);
				pwRouter.println(ssJoinMsg);
			}
			else{
				System.out.println("connection is not there --> so establish for SSJOIn  router:"+nextHopRouter);
				EstablishConnectionToNextRouter(routerId,nextHopRouter,mgroupId);
				PrintWriter pw = routerPWMap.get(nextHopRouter);
				pw.println(ssJoinMsg);
			}

		}

		System.out.println("---------------------------End of doSSJoinOperation-----------------------------");
	}
	/**
	 * This method is used to update the routing tables once it receives the SSJOIN messages from the RP
	 * @param frmRouter
	 * @param srcId
	 * @param mgroupId
	 */
	private void updateForwardingSSJoinTable(String frmRouter, String srcId,String mgroupId) {
		// TODO Auto-generated method stub
		System.out.println("---------------------------Inside updateForwardingSSJoinTable-----------------------------");
		String ssJoinEntry = String.format("(%s,%s) %s - \n", srcId,mgroupId,frmRouter);
		String Path = Router.resourceBundle.getString("PATH")+"routers//Router"+routerId;
		String fileContent =Router.readFile(Path);
		fileContent = fileContent + ssJoinEntry;
		writeToFile(Path,fileContent);


		System.out.println("---------------------------End of updateForwardingSSJoinTable-----------------------------");
	}
	/**
	 * This method is used to send the REGISTER message once it receives the SEND message from the host.
	 * @param srcId
	 * @param rendzPt
	 * @param mgroupId
	 * @param data
	 * @throws IOException
	 */
	private void doRegisterOperation(String srcId, String rendzPt,String mgroupId, String data) throws IOException {
		// TODO Auto-generated method stub
		System.out.println("---------------------------Inside doRegisterOperation-----------------------------");
		String nextHopRouter;
		if(routerId.trim().equalsIgnoreCase(rendzPt)){
			// rendezpoint here
			System.out.println("Reached the rendezous point !!!!!!!! REGISTER MSG IS RECEIVED TO RENDZ POINT.");
			sendSSJoinMsg(srcId,mgroupId);
			try {
				Thread.sleep(1000);
				//send MCast msg to every1 in the tree
				sendMCastMsg(srcId,mgroupId,data);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			//dont know wat to do here
			//updateJoinForwardingTable(frmRouter,rendzPt,mgroupId);
		}
		else{
			// not rendezous point so calculate dijstras
			System.out.println("NOT A RENDEZOUS POINT  SO CALCULATING DIJKSTRAS");
			System.out.println("here source:"+routerId+" trim:"+rendzPt);
			nextHopRouter = fetchNextHopUsingDijkstra(routerId,rendzPt);
			PrintWriter pwRouter = routerPWMap.get(nextHopRouter);
			if(pwRouter != null){
				System.out.println("connection is already there--> so REGISTER FORWARD regMsg:"+msgToServer);
				pwRouter.println(msgToServer);
			}
			else{
				System.out.println("connection is not there --> so establish to Register  router:"+nextHopRouter);
				EstablishConnectionToNextRouter(routerId,nextHopRouter,mgroupId);
				PrintWriter pw = routerPWMap.get(nextHopRouter);
				pw.println(msgToServer);
			}
		}
		System.out.println("---------------------------End of doRegisterOperation-----------------------------");
	}
	/**
	 * This method is used to forward the MCAST message to the hosts/routers present in its routing table.
	 * @param srcId
	 * @param mgroupId
	 * @param data
	 * @throws IOException
	 */
	private void sendMCastMsg(String srcId, String mgroupId,String data) throws IOException {
		// TODO Auto-generated method stub
		System.out.println("---------------------------Inside sendMCastMsg-----------------------------");
		String mCastMsg = String.format("MCAST %s %s %s %s", routerId,srcId,mgroupId,data);

		//Parse the MCast tree
		checkMCastTreeToSendMCast(srcId,mgroupId,data,mCastMsg);
		System.out.println("---------------------------End of sendMCastMsg-----------------------------");
	}
	/**
	 * This method is used to forward the MCAST message to the hosts/routers present in its routing table.
	 * @param srcId
	 * @param mgroupId
	 * @param data
	 * @param mcastMsg
	 * @throws IOException
	 */
	private void checkMCastTreeToSendMCast(String srcId,String mgroupId, String data, String mcastMsg) throws IOException {
		// TODO Auto-generated method stub
		System.out.println("---------------------------Start of checkMCastTreeToSendMCast-----------------------------");

		String Path = Router.resourceBundle.getString("PATH")+"routers//Router"+routerId;
		File file = new File(Path);
		System.out.println("Inside checkMCastTreeToSendMCast:"+mcastMsg);
		if(!file.exists()){
			System.out.println("File not exists "+routerId);
		}
		else{
			//file exists so check for multicast group in the file
			System.out.println("File exists so check for multicast group in the file");
			String fileContent =Router.readFile(Path);
			String routerArr[] = fileContent.split("\n");
			boolean isMCastPresent=false;
			String mCastIdInFile;
			String mcastRow=null;
			for(int i=0;i<routerArr.length;i++){
				if(routerArr[i].contains("*,")){
					mCastIdInFile = routerArr[i].split("\\*,")[1].split("\\)")[0];
					if(mCastIdInFile.equalsIgnoreCase(mgroupId.trim())){
						System.out.println("Found muticast row in routing table checkMCastTreeToSendMCast--> so check for hostid");
						mcastRow = routerArr[i];
						isMCastPresent = true;
						//break;
					}
				}
			}
			if(isMCastPresent){
				// already present 
				// host is same, then do nothing if not add it to the hostlist
				sendMCastFromRoutingTable(mcastRow,1,mcastMsg,routerPWMap,mgroupId);
				sendMCastFromRoutingTable(mcastRow,2,mcastMsg,hostPWMap,mgroupId);
			}
			else{
				System.out.println("MCast In routing not present");
			}
		}
		System.out.println("---------------------------End of checkMCastTreeToSendMCast-----------------------------");		
	}
	/**
	 * This method is used to read and parse the routing table to send the MCAST messages.
	 * @param mcastRow
	 * @param column
	 * @param mcastMsg
	 * @param map
	 * @param mgroupId
	 * @throws IOException
	 */
	private void sendMCastFromRoutingTable(String mcastRow, int column, String mcastMsg, HashMap<String, PrintWriter> map, String mgroupId) throws IOException {
		// TODO Auto-generated method stub
		System.out.println("---------------------------Start of sendMCastFromRoutingTable-----------------------------");
		String nextHopRList = mcastRow.split(" ")[column];
		if(nextHopRList.contains(",")){
			String hosttList[] = nextHopRList.split(",");
			PrintWriter pwRouter;
			for(int i=0;i<hosttList.length;i++){
				pwRouter = map.get(hosttList[i]);
				if(pwRouter != null){
					System.out.println("connection is already there--> so send MCAST:"+mcastMsg);
				}
				else{
					System.out.println("connection is not there --> so establish to MCAST  router:"+mcastMsg);
					EstablishConnectionToNextRouter(routerId,hosttList[i],mgroupId);
					pwRouter = map.get(hosttList[i]);
				}
				pwRouter.println(mcastMsg);
			}
		}
		else{
			// only 1 host present
			String mcastArr[] = mcastRow.split(" ");
			if(mcastArr[column].trim().contains("-")){
				System.out.println("contains - case --- NO Next Entry present --- so dont do anything");
			}
			else{
				System.out.println("doesnt contain - case. Send Mcast to Next one:"+mcastArr[column]);
				PrintWriter pwRouter = map.get(mcastArr[column]);
				if(pwRouter != null){
					System.out.println("connection is already there--> so send MCAST:"+mcastMsg);
				}
				else{
					System.out.println("connection is not there --> so establish to MCAST  :"+mcastMsg);
					EstablishConnectionToNextRouter(routerId,mcastArr[column],mgroupId);
					pwRouter = map.get(mcastArr[column]);
				}
				pwRouter.println(mcastMsg);
			}
		}
		System.out.println("---------------------------End of sendMCastFromRoutingTable-----------------------------");
	}
	/**
	 * THis method is used to send the SSJOIN message once RP receives the REGISTER message.
	 * @param srcId
	 * @param mgroupId
	 * @throws IOException
	 */
	private void sendSSJoinMsg(String srcId, String mgroupId) throws IOException {
		// TODO Auto-generated method stub
		System.out.println("---------------------------Inside sendSSJoinMsg-----------------------------");
		String nextHopRouter;
		String ssJoinMsg = String.format("SSJOIN %s %s %s", routerId,srcId,mgroupId);
		nextHopRouter = fetchNextHopUsingDijkstra(routerId,srcId);

		PrintWriter pwRouter = routerPWMap.get(nextHopRouter);
		if(pwRouter != null){
			System.out.println("connection is already there--> so SSJOIN  ssJoinMsg:"+ssJoinMsg);
			pwRouter.println(ssJoinMsg);
		}
		else{
			System.out.println("connection is not there --> so establish to SSJOIN  router:"+ssJoinMsg);
			EstablishConnectionToNextRouter(routerId,nextHopRouter,mgroupId);
			PrintWriter pw = routerPWMap.get(nextHopRouter);
			pw.println(ssJoinMsg);
		}

		System.out.println("---------------------------End of sendSSJoinMsg-----------------------------");
	}
	/**
	 * THis method is used to send the PRUNE message once the host sends LEAVE message.
	 * @param frmRouter
	 * @param rendzPt
	 * @param mgroupId
	 */
	private void doPruneOperation(String frmRouter, String rendzPt,String mgroupId) {
		// TODO Auto-generated method stub
		System.out.println("---------------------------Inside doPruneOperation-----------------------------");

		String Path = Router.resourceBundle.getString("PATH")+"routers//Router"+routerId;
		File file = new File(Path);
		String delEntry;
		if(!file.exists()){
			System.out.println("File not exists");
		}
		else{
			//file exists so check for multicast group in the file
			System.out.println("File exists so check for multicast group in the file");
			String fileContent =Router.readFile(Path);
			String routerArr[] = fileContent.split("\n");
			boolean isMCastPresent=false;
			String mCastIdInFile;
			String mcastRow=null;
			for(int i=0;i<routerArr.length;i++){
				if(routerArr[i].contains("*,")){
					mCastIdInFile = routerArr[i].split("\\*,")[1].split("\\)")[0];
					if(mCastIdInFile.equalsIgnoreCase(mgroupId.trim())){
						System.out.println("Found muticast row in routing table doReportOperation --> so check for hostid");
						mcastRow = routerArr[i];
						isMCastPresent = true;
					}
				}
			}
			System.out.println("Before isMCastPresent");
			if(isMCastPresent){
				// already present 
				// host is same, then do nothing if not add it to the hostlist
				System.out.println("Multicast is present so continue dude:"+mgroupId);
				String rList = mcastRow.split(" ")[1];
				if(rList.contains(",")){
					String centerRList[] = rList.trim().split(",");
					boolean isRPresent = false;
					for(int i=0;i<centerRList.length;i++){
						if(centerRList[i].equalsIgnoreCase(frmRouter)){
							isRPresent = true;
						}
					}

					if(!isRPresent){
						// dont do anything
						System.out.println("Router entry in center column not present in routing table. So Don't do anything");
					}
					else{
						System.out.println("Host entry is present. So delete the host entry & no Prune message should be sent");
						String mcastArr[] = mcastRow.split(" ");
						delEntry = removeEntryFromTable(mcastArr[1].trim(),frmRouter);
						delEntry = mcastArr[0]+" "+delEntry+" "+mcastArr[2];
						fileContent = fileContent.replace(mcastRow,delEntry);
						writeToFile(Path,fileContent);
					}
				}
				else{
					// only 1 router present
					if(rList.trim().equalsIgnoreCase(frmRouter)){
						// deleting the entry 
						System.out.println("Found the entry --> So delete it");
						// check for other
						String mcastArr[] = mcastRow.split(" ");
						delEntry = mcastArr[1].trim().replace(frmRouter,"-");
						delEntry = mcastArr[0]+" "+delEntry+" "+mcastArr[2];
						fileContent = fileContent.replace(mcastRow,delEntry);
						writeToFile(Path,fileContent);
						//send prune message to Next Router
						if(mcastArr[2].contains("-")){
							// no more router entries present in routing table
							System.out.println("No more entries in routing table present--> so sending prune towards rendez point");
							sendPruneToNextRouter(mgroupId,rendzPt);
						}
						else{
							System.out.println("Entries in the Next host are present --> Not sending prune");
						}
					}
					else{
						// this shouldn't happen coz the host already should be in host table
						System.out.println("Router entry is not present in routing table");
					}
				}
			}
			else{
				System.out.println("MCast In routing not present");
			}
			System.out.println("REport DOne doing it ");
		}

		System.out.println("---------------------------End of doPruneOperation-----------------------------");
	}
	/**
	 * This method is used to send the REGISTER message once host sends the SEND message.
	 * @param hostId
	 * @param mgroupId
	 * @param data
	 * @throws IOException
	 */
	private void doSendOperation(String hostId, String mgroupId, String data) throws IOException {
		// TODO Auto-generated method stub
		System.out.println("---------------------------Inside doSendOperation-----------------------------");
		String nextHopRouter;
		boolean isSSJoinPresent = checkSSJoinEntryInRTable(mgroupId);
		if(!isSSJoinPresent){
			System.out.println("ssJOIN is not there -->send REGISTER to rendz point");
			String rendezPoint = rpMap.get(mgroupId);
			String regMsg = String.format("REGISTER %s %s %s %s", routerId,rendezPoint,mgroupId,data);
			if(routerId.trim().equalsIgnoreCase(rendezPoint)){
				// rendezpoint here
				System.out.println("Reached the rendezous point !!!!!!!! REGISTER MSG IS RECEIVED TO RENDZ POINT.");
				sendSSJoinMsg(routerId,mgroupId);

				//dont know wat to do here
				//updateJoinForwardingTable(frmRouter,rendzPt,mgroupId);
			}
			else{
				// not rendezous point so calculate dijstras
				System.out.println("NOT A RENDEZOUS POINT  SO CALCULATING DIJKSTRAS");
				System.out.println("here source:"+routerId+" trim:"+rendezPoint);
				nextHopRouter = fetchNextHopUsingDijkstra(routerId,rendezPoint);
				PrintWriter pwRouter = routerPWMap.get(nextHopRouter);
				if(pwRouter != null){
					System.out.println("connection is already there--> so send regMsg:"+regMsg);
					pwRouter.println(regMsg);
				}
				else{
					System.out.println("connection is not there --> so establish to nexthop router:"+nextHopRouter);
					EstablishConnectionToNextRouter(routerId,nextHopRouter,mgroupId);
					PrintWriter pw = routerPWMap.get(nextHopRouter);
					pw.println(regMsg);
				}

			}
		}
		else{
			System.out.println("SSJoin is present here");
		}
		System.out.println("---------------------------End of doSendOperation-----------------------------");
	}
	/**
	 * This method is used to check the ssjoin router entry.
	 * @param mgroupId
	 * @return
	 */
	private boolean checkSSJoinEntryInRTable(String mgroupId) {
		// TODO Auto-generated method stub
		String Path = Router.resourceBundle.getString("PATH")+"routers//Router"+routerId;
		String fileContent =Router.readFile(Path);
		String routerArr[] = fileContent.split("\n");
		boolean isSSJoinPresent=false;
		String mcastRow=null;
		String ssEntry = String.format("(%s,%s)", routerId,mgroupId);
		for(int i=0;i<routerArr.length;i++){
			if(routerArr[i].equalsIgnoreCase(ssEntry)){
				System.out.println("Found SS JOin entry here");
				mcastRow = routerArr[i];
				isSSJoinPresent = true;
			}
		}
		return isSSJoinPresent;
	}
	/**
	 * This message is used to check whether to send PRUNE message or not depending on the routing table entries.
	 * @param hostId
	 * @param mgroupId
	 */
	private void doLeaveOperation(String hostId, String mgroupId) {
		// TODO Auto-generated method stub
		System.out.println("---------------------------Inside doLeaveOperation-----------------------------");
		String Path = Router.resourceBundle.getString("PATH")+"routers//Router"+routerId;
		File file = new File(Path);
		String delEntry;
		if(!file.exists()){
			System.out.println("File not exists");
		}
		else{
			//file exists so check for multicast group in the file
			System.out.println("File exists so check for multicast group in the file");
			String fileContent =Router.readFile(Path);
			String routerArr[] = fileContent.split("\n");
			boolean isMCastPresent=false;
			String mCastIdInFile;
			String mcastRow=null;
			for(int i=0;i<routerArr.length;i++){
				if(routerArr[i].contains("*,")){
					mCastIdInFile = routerArr[i].split("\\*,")[1].split("\\)")[0];
					if(mCastIdInFile.equalsIgnoreCase(mgroupId.trim())){
						System.out.println("Found muticast row in routing table doReportOperation --> so check for hostid");
						mcastRow = routerArr[i];
						isMCastPresent = true;						
					}
				}
			}
			System.out.println("Before isMCastPresent");
			if(isMCastPresent){
				// already present 
				// host is same, then do nothing if not add it to the hostlist
				System.out.println("Multicast is present so continue dude:"+mgroupId);
				String hList = mcastRow.split(" ")[2];
				if(hList.contains(",")){
					String hosttList[] = hList.trim().split(",");
					boolean isHostPresent = false;
					for(int i=0;i<hosttList.length;i++){
						if(hosttList[i].equalsIgnoreCase(hostId)){
							isHostPresent = true;
						}
					}

					if(!isHostPresent){
						// dont do anything
						System.out.println("Host entry not present in routing table. So don't do anything");
					}
					else{
						System.out.println("Host entry is present. So delete the host entry & no Prune message should be sent");
						String mcastArr[] = mcastRow.split(" ");
						delEntry = removeEntryFromTable(mcastArr[2].trim(),hostId);
						delEntry = mcastArr[0]+" "+mcastArr[1]+" "+delEntry;
						fileContent = fileContent.replace(mcastRow,delEntry);
						writeToFile(Path,fileContent);
					}
				}
				else{
					// only 1 host present
					if(hList.trim().equalsIgnoreCase(hostId)){
						// deleting the entry 
						System.out.println("Found the entry --> So delete it");
						// check for other
						String mcastArr[] = mcastRow.split(" ");
						delEntry = mcastArr[2].trim().replace(hostId,"-");
						delEntry = mcastArr[0]+" "+mcastArr[1]+" "+delEntry;
						fileContent = fileContent.replace(mcastRow,delEntry);
						writeToFile(Path,fileContent);

						//send prune message to Next Router
						if(mcastArr[1].contains("-")){
							// no more router entries present in routing table
							System.out.println("No more entries in routing table present--> so sending prune towards rendez point");
							String rendezPoint = rpMap.get(mgroupId);
							sendPruneToNextRouter(mgroupId,rendezPoint);
						}
						else{
							System.out.println("Entries in the next hop routing table are present .Not sending prune");
						}
					}
					else{
						// this shouldn't happen coz the host already should be in host table
						System.out.println("Host entry is not present in routing table");
					}
				}
			}
			else{
				System.out.println("MCast In routing not present--> its wrong dude check urself ");
			}
			System.out.println("REport DOne doing it ");
		}
	}
	/**
	 * This code is used to remove the entry from the routing table.
	 * @param commaList
	 * @param removeItem
	 * @return
	 */
	private String removeEntryFromTable(String commaList,String removeItem) {
		// TODO Auto-generated method stub
		commaList = commaList.replace(removeItem, "").replace(",,", ",");
		if(commaList.endsWith(",")){
			commaList = commaList.substring(0, commaList.length()-1);
			System.out.println("ends with:"+commaList);
		}
		else if(commaList.startsWith(","))
		{
			commaList = commaList.substring(1, commaList.length());
			System.out.println("starts with:"+commaList);
		}
		System.out.println("commaList:"+commaList);
		return commaList;
	}
	/**
	 * This method is used to send the PRUNE message to the next router.
	 * @param mgroupId
	 * @param rendzPt
	 */
	private void sendPruneToNextRouter(String mgroupId, String rendzPt) {
		// TODO Auto-generated method stub
		System.out.println("--------------Inside sendPruneToNextRouter---------------");


		if(routerId.trim().equalsIgnoreCase(rendzPt)){
			// rendezpoint here
			System.out.println("Reached the rendezous point for PRUNE !!!!!!!! PATH IS removed completely.");
			//updateReportForwardingTable(hostId,mgroupId);

		}
		else
		{
			// not rendezous point so calculate dijstras
			System.out.println("NOT A RENDEZOUS POINT  SO CALCULATING DIJKSTRAS");
			//Dijkstras algorithm here
			String nextHopRouter;
			nextHopRouter = fetchNextHopUsingDijkstra(routerId,rendzPt);
			System.out.println("check nextHopRouter:"+nextHopRouter);
			PrintWriter pw =  routerPWMap.get(nextHopRouter);
			pw.println(String.format("PRUNE %s %s %s", routerId,rendzPt,mgroupId));
		}	
		System.out.println("--------------End of sendPruneToNextRouter---------------");

	}
	/**
	 * This method is used to handle the REPORT message received from the host.
	 * @param hostId
	 * @param mgroupId
	 * @throws IOException
	 */
	private void doReportOperation(String hostId, String mgroupId) throws IOException {
		// TODO Auto-generated method stub
		System.out.println("---------------------------Inside doReportOperation-----------------------------");
		ArrayList<String> arrList = new ArrayList<String>();
		boolean isHostJoined= false;
		boolean isRouterMultiCastMem = false;
		java.util.Iterator<String> hashSetItr = hostList.iterator();
		while(hashSetItr.hasNext()){
			if(hashSetItr.next().equalsIgnoreCase(hostId)){
				System.out.println("Host already joined !!!!!!!!!!!!!!");
				isHostJoined = true;
			}
		}
		if(!isHostJoined){
			hostList.add(hostId);
		}
		//check whether router is already a member of multicast group

		isRouterMultiCastMem = Router.CheckRouterMemberShipInfo();
		if(!isRouterMultiCastMem){
			// Not a member send join messages
			String rendezPoint = rpMap.get(mgroupId);
			System.out.println("routerId:"+routerId+" rendezPoint:"+rendezPoint);

			if(routerId.trim().equalsIgnoreCase(rendezPoint)){
				// rendezpoint here
				System.out.println("Reached the rendezous point !!!!!!!! PATH IS ESTABLISHED TO RENDEZOUS POINT .");
				updateReportForwardingTable(hostId,mgroupId);

			}
			else
			{
				// not rendezous point so calculate dijstras
				System.out.println("NOT A RENDEZOUS POINT  SO CALCULATING DIJKSTRAS");

				//Dijkstras algorithm here
				String nextHopRouter;
				nextHopRouter = fetchNextHopUsingDijkstra(routerId,rendezPoint);

				System.out.println("nextHopRouter:"+nextHopRouter);
				boolean isConnectNeeded = updateReportForwardingTable(hostId, mgroupId);
				if(isConnectNeeded){
					EstablishConnectionToNextRouter(routerId,nextHopRouter,mgroupId);
					PrintWriter pw = routerPWMap.get(nextHopRouter);
					pw.println(String.format("JOIN %s %s %s", routerId,rendezPoint,mgroupId));
				}
			}	
		}
		System.out.println("---------------------------END OF doReportOperation-----------------------------");
	}
	/**
	 * This method is used to fetch the next hop router to the RP/Source router using the dijkstra's algorithm.
	 * @param rId
	 * @param rendezPoint
	 * @return
	 */
	private String fetchNextHopUsingDijkstra(String rId,String rendezPoint) {
		// TODO Auto-generated method stub
		String nextHopRouter;
		List list =  (List) Dijkstra.getDijkstraInfo(Integer.parseInt(rId.trim()), Integer.parseInt(rendezPoint.trim()));
		//Fetch the next hop router
		if(list.size() > 1)
			nextHopRouter = list.get(1).toString();
		else{
			// HANDLED THE CASE SHOULDNT ENTER HERE
			System.out.println("CASE IS HANDLED");
			nextHopRouter = list.get(0).toString();
		}
		nextHopRouter = nextHopRouter.replace("R", "");
		return nextHopRouter;
	}
	/**
	 * This method is used to update the Routing table once it receives the REPORT message.
	 * @param hostId
	 * @param mgroupId
	 * @return
	 */
	private boolean updateReportForwardingTable(String hostId, String mgroupId) {
		// TODO Auto-generated method stub
		boolean isToEstablishConn = false;
		String Path = Router.resourceBundle.getString("PATH")+"routers//Router"+routerId;
		File file = new File(Path);
		String fwrdEntry;
		fwrdEntry = String.format("(*,%s) - %s \n",mgroupId,hostId);
		System.out.println("Inside Report fwrdEntry:"+fwrdEntry);
		if(!file.exists()){
			System.out.println("File not exists --> So create Router file for router id:"+routerId);
			writeToFile(Path,fwrdEntry);
			// establish connection
			isToEstablishConn = true;
			//EstablishConnectionToNextRouter(routerId,nextHopRouter,mgroupId);
		}
		else{
			//file exists so check for multicast group in the file
			System.out.println("File exists so check for multicast group in the file");
			String fileContent =Router.readFile(Path);
			String routerArr[] = fileContent.split("\n");
			boolean isMCastPresent=false;
			String mCastIdInFile;
			String mcastRow=null;
			for(int i=0;i<routerArr.length;i++){
				if(routerArr[i].contains("*,")){
					mCastIdInFile = routerArr[i].split("\\*,")[1].split("\\)")[0];
					if(mCastIdInFile.equalsIgnoreCase(mgroupId.trim())){
						System.out.println("Found muticast row in routing table doReportOperation --> so check for hostid");
						mcastRow = routerArr[i];
						isMCastPresent = true;
						//break;
					}
				}
			}
			System.out.println("Before isMCastPresent");
			if(isMCastPresent){
				// already present 
				// host is same, then do nothing if not add it to the hostlist
				System.out.println("Multicast is present so continue dude:"+mgroupId);
				String hList = mcastRow.split(" ")[2];
				if(hList.contains(",")){
					String hosttList[] = hList.split(",");
					boolean isHostPresent = false;
					for(int i=0;i<hosttList.length;i++){
						if(hosttList[i].equalsIgnoreCase(hostId)){
							isHostPresent = true;
						}
					}

					if(!isHostPresent){
						//add the entry & establish the connection
						fwrdEntry = fwrdEntry+","+hostId;
						fileContent = fileContent.replace(mcastRow,fwrdEntry);
						writeToFile(Path,fileContent);
						isToEstablishConn = true;
						//EstablishConnectionToNextRouter(routerId,nextHopRouter,mgroupId);
					}
					else{
						System.out.println("PATH OVERRR");
					}
				}
				else{
					// only 1 host present
					if(hList.trim().equalsIgnoreCase(hostId)){
						// no need to establish connection. DOnt do anything
						System.out.println("PATH IS DONE !!");
					}
					else{
						// Add the entry & establish the connection

						String mcastArr[] = mcastRow.split(" ");
						if(mcastArr[2].trim().contains("-")){
							System.out.println("contains - case report");
							fwrdEntry = mcastArr[2].trim().replace("-", hostId);
						}
						else{
							System.out.println("doesnt contain - report");
							fwrdEntry = mcastArr[2].trim()+","+hostId;
						}
						fwrdEntry = mcastArr[0]+" "+mcastArr[1]+" "+fwrdEntry;
						fileContent = fileContent.replace(mcastRow,fwrdEntry);
						writeToFile(Path,fileContent);
						//connection establish + send JOin message
						isToEstablishConn = true;
						//EstablishConnectionToNextRouter(routerId,nextHopRouter,mgroupId);
					}
				}
			}
			else{
				System.out.println("MCast In routing not present--> Adding entry ");
				fileContent = fileContent + fwrdEntry;
				writeToFile(Path,fileContent);
				isToEstablishConn = true;
				//EstablishConnectionToNextRouter(routerId,nextHopRouter,mgroupId);
			}
			System.out.println("REport DOne doing it ");
		}

		return isToEstablishConn;
	}
	/**
	 * This method will handle and process the JOIN message received to the server.
	 * @param frmRouter
	 * @param rendzPt
	 * @param mgroupId
	 * @throws IOException
	 */
	private void doJoinOperation(String frmRouter, String rendzPt, String mgroupId) throws IOException {
		// TODO Auto-generated method stub
		System.out.println("---------------Inside doJoinOperation----------------------------");
		String nextHopRouter = null;

		if(routerId.trim().equalsIgnoreCase(rendzPt)){
			// rendezpoint here
			System.out.println("Reached the rendezous point !!!!!!!! PATH IS ESTABLISHED TO RENDEZOUS POINT .");
			updateJoinForwardingTable(frmRouter,rendzPt,mgroupId);
		}
		else{
			// not rendezous point so calculate dijstras
			System.out.println("NOT A RENDEZOUS POINT  SO CALCULATING DIJKSTRAS");
			System.out.println("here source:"+routerId+" trim:"+rendzPt);
			nextHopRouter = fetchNextHopUsingDijkstra(routerId,rendzPt);
			boolean connEstaCheck = updateJoinForwardingTable(frmRouter,rendzPt,mgroupId);
			if(connEstaCheck){
				EstablishConnectionToNextRouter(routerId,nextHopRouter,mgroupId);
				PrintWriter pw = routerPWMap.get(nextHopRouter);
				pw.println(String.format("JOIN %s %s %s", routerId,rendzPt,mgroupId));
			}

		}
		System.out.println("---------------------------END OF doJOINOperation-----------------------------");
	}
	/**
	 * This method is used to update the routing table once it receives the JOIN message from another router.
	 * @param frmRouter
	 * @param rendzPt
	 * @param mgroupId
	 * @return
	 */
	private boolean updateJoinForwardingTable(String frmRouter, String rendzPt, String mgroupId) {
		// TODO Auto-generated method stub
		String fwrdEntry;
		boolean isToEstablishConn = false;
		String Path = Router.resourceBundle.getString("PATH")+"routers//Router"+routerId;
		File file = new File(Path);
		fwrdEntry = String.format("(*,%s) %s - \n",mgroupId,frmRouter);
		System.out.println("Inside JOIN fwrdEntry:"+fwrdEntry);
		if(!file.exists()){
			System.out.println("File not exists --> So create Router file for router id:"+routerId);
			writeToFile(Path,fwrdEntry);
			isToEstablishConn = true;
			// establish connection
			//EstablishConnectionToNextRouter(routerId,nextHopRouter,mgroupId);
		}
		else{
			//file exists so check for multicast group in the file
			System.out.println("File exists so check for multicast group in the file");
			String fileContent =Router.readFile(Path);
			String routerArr[] = fileContent.split("\n");
			boolean isMCastPresent=false;
			String mCastIdInFile;
			String mcastRow=null;
			for(int i=0;i<routerArr.length;i++){
				if(routerArr[i].contains("*,")){
					mCastIdInFile = routerArr[i].split("\\*,")[1].split("\\)")[0];
					if(mCastIdInFile.equalsIgnoreCase(mgroupId.trim())){
						System.out.println("Found muticast row in routing table doJoinOperation--> so check for hostid");
						mcastRow = routerArr[i];
						isMCastPresent = true;
						//break;
					}
				}
			}
			if(isMCastPresent){
				// already present 
				// host is same, then do nothing if not add it to the hostlist
				String nextHopRList = mcastRow.split(" ")[1];
				if(nextHopRList.contains(",")){
					String hosttList[] = nextHopRList.split(",");
					boolean isNextHopRouterPresent = false;
					for(int i=0;i<hosttList.length;i++){
						if(hosttList[i].equalsIgnoreCase(frmRouter)){
							isNextHopRouterPresent = true;
						}
					}

					if(!isNextHopRouterPresent){
						//add the entry & establish the connection
						String mcastArr[] = mcastRow.split(" ");
						fwrdEntry = mcastArr[1].trim()+","+frmRouter;
						fwrdEntry = mcastArr[0]+" "+fwrdEntry+" "+mcastArr[2];
						fileContent = fileContent.replace(mcastRow,fwrdEntry);
						//fileContent = fileContent+fwrdEntry.trim();
						writeToFile(Path,fileContent);
						isToEstablishConn = true;
						//EstablishConnectionToNextRouter(routerId,nextHopRouter,mgroupId);
					}
					else{
						System.out.println("CONNECTION IS ALREADY ESTABLISHD");
					}
				}
				else{
					// only 1 host present
					if(nextHopRList.trim().equalsIgnoreCase(frmRouter)){
						// no need to establish connection. DOnt do anything
						System.out.println("PATH IS ALREADY PRESENT !!!");
					}
					else{
						// Add the entry & establish the connection
						String mcastArr[] = mcastRow.split(" ");
						if(mcastArr[1].trim().contains("-")){
							System.out.println("contains - case");
							fwrdEntry = mcastArr[1].trim().replace("-", frmRouter);
						}
						else{
							System.out.println("doesnt contain -");
							fwrdEntry = mcastArr[1].trim()+","+frmRouter;
						}
						fwrdEntry = mcastArr[0]+" "+fwrdEntry+" "+mcastArr[2];
						fileContent = fileContent.replace(mcastRow,fwrdEntry);
						writeToFile(Path,fileContent);
						//connection establish + send JOin message
						isToEstablishConn = true;
						//EstablishConnectionToNextRouter(routerId,nextHopRouter,mgroupId);
					}
				}
			}
			else{
				System.out.println("MCast In routing not present--> Adding entry ");
				fileContent = fileContent + fwrdEntry;
				writeToFile(Path,fileContent);
				isToEstablishConn = true;
				//EstablishConnectionToNextRouter(routerId,nextHopRouter,mgroupId);
			}
		}

		return isToEstablishConn;
	}

	/**
	 * This method is used to establish the connection to the router calculated by dijkstra's algorithm.
	 * @param routerId
	 * @param nextHopRouter
	 * @param mgroupId
	 * @throws IOException
	 */
	public void EstablishConnectionToNextRouter(final String routerId,final String nextHopRouter, final String mgroupId) throws IOException {
		// TODO Auto-generated method stub	
		String configContent = Router.readFile(Router.resourceBundle.getString("PATH")+"Config.props").trim();
		String routerList [] = configContent.split("\n");
		System.out.println("Establishing connection to nextHopRouter:"+nextHopRouter);

		String routerInfo = routerList[Integer.parseInt(nextHopRouter)];
		String routerArr[] = routerInfo.split(" ");
		String routerIp = routerArr[1];
		int routerPort = Integer.parseInt(routerArr[2]);
		InetAddress routerIPAddr = InetAddress.getByName(routerIp);  // Fetching the IP address from the hostname
		System.out.println(routerIPAddr.toString()+routerPort);
		final Socket routerSocket = new Socket(routerIPAddr, routerPort);

		Thread senderThread  = new Thread(){
			public void run(){
				//BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));  //creating object for bufferreader
				System.out.println("Inside senderThread run");
				String userInput = null;
				PrintWriter pwRouter = null;
				try {
					pwRouter = new PrintWriter(routerSocket.getOutputStream(), true);
					System.out.println("Router connection established --- Maintain Map");

					routerPWMap = maintainPrintWriMap(routerPWMap,nextHopRouter,pwRouter);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		};
		senderThread.start();
		//join the threads
		try {
			senderThread.join();
			//receiverThread.join();
			System.out.println("Joined the threads");
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		//routerSocket.close();
	}
	/**
	 * This method is used to maintain the different TCP connections established along the flow.
	 * @param pwmap
	 * @param id
	 * @param pw
	 * @return
	 */
	private HashMap<String, PrintWriter> maintainPrintWriMap(HashMap<String, PrintWriter> pwmap, String id, PrintWriter pw) {
		// TODO Auto-generated method stub

		Iterator<String> keyItr = pwmap.keySet().iterator();
		String eachHost;
		boolean isIdPresent = false;
		if(keyItr.hasNext()){
			eachHost = keyItr.next();
			if(eachHost.equalsIgnoreCase(id)){
				isIdPresent = true;
			}
		}
		if(!isIdPresent){
			System.out.println("host not present--> add PR to map id:"+id+" pwriter:"+pw.toString());
			pwmap.put(id, pw);
		}
		return pwmap;
	}
	/**
	 * THis method is used to write the messages to different connections.
	 * @param fileName
	 * @param content
	 */
	public void writeToFile(String fileName,String content){
		try{
			// Create file 
			FileWriter fstream = new FileWriter(fileName);
			BufferedWriter out = new BufferedWriter(fstream);
			out.write(content);
			//Close the output stream
			out.close();
		}catch (Exception e){//Catch exception if any
			e.printStackTrace();
		}
	}


}