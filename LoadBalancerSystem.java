
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

class Server{
    private final String id;
    private final String host;
    private final int port;
    private final AtomicInteger activeConnections;
    private boolean isHealthy;
    public Server(String id,String host,int port){
        this.id=id;
        this.host=host;
        this.port=port;
        this.activeConnections=new AtomicInteger(0);
        this.isHealthy=true;
    }
    public void incrementConnections(){
        activeConnections.incrementAndGet();
    }
    public void decrementConnections(){
        activeConnections.decrementAndGet();
    }
    public int getActiveConnections(){
        return activeConnections.get();
    }
    public boolean isHealthy(){
        return isHealthy;
    }
    public void setHealthy(boolean isHealthy){
        this.isHealthy=isHealthy;
    }
    public String getId(){return id;}
    public String getHost(){return host;}
    public int getPort(){return port;}
    @Override
    public String toString(){
        return "id: "+id+", host: "+host+", port: "+port+", activeConnections: "+activeConnections.get()+", isHealthy: "+isHealthy;
    }
}
interface LoadBalancingStrategy{
    Server selectServer(List<Server> servers);
}

class RoundRobinStategy implements LoadBalancingStrategy{
    private final AtomicInteger index=new AtomicInteger(0);
    @Override
    public Server selectServer(List<Server> servers){
        if(servers.isEmpty()) return null;
        return servers.get(index.getAndIncrement()%servers.size());
    }
}

class LeastConnectionsStrategy implements LoadBalancingStrategy{
    @Override
    public Server selectServer(List<Server> servers){
        if(servers.isEmpty()) return null;
        Server selectedServer=servers.get(0);
        for(Server server:servers){
            if(server.getActiveConnections()<selectedServer.getActiveConnections()){
                selectedServer=server;
            }
        }
        return selectedServer;
    }
}

class ServerPool{
    private final List<Server> servers = new CopyOnWriteArrayList<>();
    public void addServer(Server server){
        servers.add(server);
        System.out.println("Server added: "+server.toString());
    }
    public void removeServer(Server serverId){
        servers.removeIf(server->server.getId().equals(serverId.getId()));
        System.out.println("Serverid removed: "+serverId);
    }
    public Server getServerById(String serverId){
       return servers.stream().filter(server->server.getId().equals(serverId)).findFirst().orElse(null);
    }
    public List<Server> getHealthyServers(){
        return servers.stream().filter(Server::isHealthy).toList();
    }
}

class LoadBalancer{
    private final ServerPool serverPool;
    private LoadBalancingStrategy strategy;
    public LoadBalancer(ServerPool serverPool, LoadBalancingStrategy strategy){
        this.serverPool=new ServerPool();
        this.strategy=strategy;
    }
    public void addServer(Server server){
        serverPool.addServer(server);
    }
    public void removeServer(Server server){
        serverPool.removeServer(server);
    }
    public void setStrategy(LoadBalancingStrategy loadBalancingStrategy){
        this.strategy=loadBalancingStrategy;
    }
    public Server routeRequest(String requestId){
        List<Server> healthyServers=serverPool.getHealthyServers();
        if(healthyServers.isEmpty()){
            System.out.println("No healthy servers available for "+requestId);
            return null;
        }
        Server selectedServer= strategy.selectServer(healthyServers);
        if(selectedServer!=null) {
            System.out.println("Request " +requestId +" routed to server: "+selectedServer.getId());
            selectedServer.incrementConnections();
        }
        return selectedServer;
    }
    public void completeRequest(String serverid){
        Server server=serverPool.getServerById(serverid);
        if(server!=null) server.decrementConnections();
    }
    public void showStatus(){
        System.out.println("Load Balancer Status:");
        for(Server server:serverPool.getHealthyServers()){
            System.out.println(server.toString());
        }
    }
}
public class LoadBalancerSystem {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("Demo Load Balancer System");
        LoadBalancer loadBalancer=new LoadBalancer(new ServerPool(), new RoundRobinStategy());
        loadBalancer.addServer(new Server("server1","192:123:3:1",8080));
        loadBalancer.addServer(new Server("server2","192.431.2.1",8080));
        loadBalancer.addServer(new Server("server3", "192.431.2.1", 8080));
        System.out.println("Round Robin Test");
        for(int i=0;i<6;i++){
            Server server = loadBalancer.routeRequest("req-"+i);
            if(server!=null) {
                Thread.sleep(50);
                loadBalancer.completeRequest(server.getId());
            }
        }
        loadBalancer.showStatus();
        System.out.println("Least Connections Test");
        loadBalancer.setStrategy(new LeastConnectionsStrategy());
        loadBalancer.routeRequest("req-7");
        loadBalancer.routeRequest("req-8");
        loadBalancer.routeRequest("req-9");
        loadBalancer.completeRequest("server1");
        loadBalancer.routeRequest("req-10"); // Should go to server1
        loadBalancer.routeRequest("req-11"); // Should go to server1

        loadBalancer.showStatus();

        System.out.println("✅ Demo complete!");
    }
}