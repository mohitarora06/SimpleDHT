# SimpleDHT

Distributed Hash Table base on CHORD with three main functionalities of CHORD

1) ID Space partitioning/ re-partitioning
2) Ring Based routing
3) Node joins

ID Space partitioning:- Key value pairs are stored in file based on the value obtained from using the SHA-1 Algorithm.
Original key is first hashed using the SHA-1 Algorithm and the hexadecimal string obtained is then lexicographically 
assigned to a particular node which lies ahead of it based on this order. 

Ring Based Routing:- For finding the appropriate node where to store the key value pair, the key-value pair is passed
to the successor based on the ring formed with hash of port number of different nodes in the system.

Node Joins:- On node join, the ring is formed by using the central coordinator which assign the location to the new 
node. 
