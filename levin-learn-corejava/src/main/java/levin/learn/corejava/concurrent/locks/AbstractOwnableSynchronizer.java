package levin.learn.corejava.concurrent.locks;

public class AbstractOwnableSynchronizer {
    private transient Thread ownerThread;
    
    protected AbstractOwnableSynchronizer() { }
    
    protected final void setOwnerThread(Thread ownerThread) {
        this.ownerThread = ownerThread;
    }
    
    protected final Thread getOwnerThread() {
        return ownerThread;
    }
}
