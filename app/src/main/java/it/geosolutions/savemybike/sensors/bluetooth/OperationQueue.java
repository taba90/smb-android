package it.geosolutions.savemybike.sensors.bluetooth;

import java.util.ArrayDeque;
import java.util.Queue;

public class OperationQueue {

    private Queue<Runnable> operations;

    private boolean queueBusy;

    public OperationQueue(){
        this.operations=new ArrayDeque<>();
    }

    public void executeCommand(){
        if(!isQueueBusy()) {
            this.queueBusy = true;
            if(!operations.isEmpty())
                operations.peek().run();
        }
    }

    public boolean isQueueBusy() {
        return queueBusy;
    }

    public void nextCommand(){
        operations.poll();
        this.queueBusy=false;
    }

    public void addCommand(Runnable command){
        this.operations.add(command);
    }
}
