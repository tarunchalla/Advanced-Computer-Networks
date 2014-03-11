Advanced-Computer-Networks
==========================

Advanced Computer Networks

Project: Multicast – PIM-SM by Srinivas(sxm125431@utdallas.edu) and Tarun(txc121730@utdallas.edu)
-------------------------------------------------------------------------------------------------
Description:  This application takes input from the user such as join, leave, send and list. 
(i) Whenever user enters a join command the router connected to it joins itself to the rendezvous point corresponding to the multicast group id. On it's way to RP it Creates/updates the routing tables. 
(ii) Whenever user enters a leave command the router removes itself from the multicasting group by sending the prune message towards the RP if none of the hosts connected to it are members of the same multicast group and deletes the entry from the table. If there are some hosts attached to the router are members of the same multicast group then it just removes the host entry from the routing table.
(iii) whenever user enters a list command, it will display the membership of the host to the different multicast groups.
(iv) whenever user enters a send command it will check for the source specific entry in the routing table and if the entry is not present then it sends the register message to the RP, RP sends back SSJOIN message to the source router and on its way insert the ssjoin entry in the routing table, and then RP sends the MCAST message to all the members of the multicast group id.
If source specific entry is present in the routing table then it directly sends the mcast message to the members of the multicast group.

Platform/compiler: UNIX, javac

Files:

TCPHost_sockets.java - Source code for Client/Host.
Router.java - Source code for Router/Server application.
MultiServerThread.java - Source code for handling threads in the Router.
Dijkstra.java - Source code for calculating Dijkstra's Algorithm to find next hop router.

Compiling instructions:

- To compile, UnZip the file "sxm125431_txc121730.zip", Place all files within a single directory in linux box and type "javac *.java".

Running instructions:
- Create a folder called "routers"  
- Change the path in PIMMulticast.properties to present working directory path.
- Run these on the netxx.utdallas.edu machines, where xx ranges from 00-45.
- Type "java Router router <routerID> <configfile> <config-rp> <config-topo>" on the server host.  
- Type "java TCPHost_sockets host <hostID> <configfile> <myrouterID> <mgroup>" 
- To exit the client or server, type CTRL-C.

- The project can be verified with the sample config, configTOPO and configRP provided by Prof:Jason Jue.

For instance:
On net01.utdallas.edu box, execute this command- 
	java Router router 0 Config.props ConfigRP.props ConfigTopo.props
Similarly run other routers on different netXX.utdallas.edu boxes as specified in the Config.props

On any other instance of linux box, execute this command- 
	java TCPHost_sockets host 0 Config.props 0 3
Similarly run other client programs on different netXX.utdallas.edu boxes as required.

And you can give all the host commands in any of the host boxes and check for the outputs and you will find Routing tables getting created and updated in Routers folder.

