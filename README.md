# zkbrowser
a war,java web application,browser of zookeeper<br/>
用springmvc,zkclient,bootstrap做的一个war，来浏览zookeeper<br/>
可以是单个zk，例：127.0.0.1:2181<br/>
可以是集群zk，例：127.0.0.1:2181,127.0.0.1:2182,127.0.0.1:2183<br/>
不支持带密码的zk<br/>
打个war包，放到tomcat下启动，访问http://127.0.0.1:8080/zkbrowser就可以了<br/>
