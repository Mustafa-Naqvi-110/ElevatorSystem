import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

class ElevatorDoor {
    private boolean isOpen = false;

    public void open() throws InterruptedException {
        if (!isOpen) {
            System.out.println("Opening door...");
            Thread.sleep(1500);
            isOpen = true;
        }
    }

    public void close() throws InterruptedException {
        if (isOpen) {
            System.out.println("Closing door...");
            Thread.sleep(1500);
            isOpen = false;
        }
    }
}

abstract class ElevatorRequest {
    protected final int targetFloor;

    public ElevatorRequest(int targetFloor) {
        this.targetFloor = targetFloor;
    }

    public int getTargetFloor() {
        return targetFloor;
    }
}

class InternalRequest extends ElevatorRequest {
    public InternalRequest(int targetFloor) {
        super(targetFloor);
    }
}

class ExternalRequest extends ElevatorRequest {
    public enum Direction { UP, DOWN }
    private final Direction direction;

    public ExternalRequest(int targetFloor, Direction direction) {
        super(targetFloor);
        this.direction = direction;
    }

    public Direction getDirection() {
        return direction;
    }
}

class Elevator implements Runnable {
    private final String id;
    private int currentFloor;
    private final ElevatorDoor door;
    private final BlockingQueue<ElevatorRequest> requests;
    private boolean operational;
    private Thread operationThread;

    public Elevator(String id) {
        this.id = id;
        this.currentFloor = 0;
        this.door = new ElevatorDoor();
        this.requests = new LinkedBlockingQueue<>();
        this.operational = true;
    }

    public void addRequest(ElevatorRequest request) {
        requests.add(request);
        System.out.println("[" + id + "] Received request for floor " + request.getTargetFloor());
    }

    public int getCurrentFloor() {
        return currentFloor;
    }

    public void shutdown() {
        operational = false;
        if (operationThread != null) {
            operationThread.interrupt();
        }
    }

    @Override
    public void run() {
        operationThread = Thread.currentThread();
        while (operational) {
            try {
                if (!requests.isEmpty()) {
                    processNextRequest();
                } else {
                    Thread.sleep(100);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        System.out.println("[" + id + "] Elevator stopped");
    }

    private void processNextRequest() throws InterruptedException {
        ElevatorRequest request = requests.take();
        System.out.println("[" + id + "] Processing request to floor " + request.getTargetFloor());
        
        while (currentFloor != request.getTargetFloor()) {
            moveTowards(request.getTargetFloor());
        }
        
        handleDoorOperation();
    }

    private void moveTowards(int targetFloor) throws InterruptedException {
        int direction = Integer.compare(targetFloor, currentFloor);
        currentFloor += direction;
        Thread.sleep(1000);
        System.out.println("[" + id + "] Now at floor " + currentFloor);
    }

    private void handleDoorOperation() throws InterruptedException {
        door.open();
        Thread.sleep(2000);
        door.close();
    }
}

class ElevatorSystem {
    private final List<Elevator> elevators;

    public ElevatorSystem(List<Elevator> elevators) {
        this.elevators = elevators;
        startSystem();
    }

    private void startSystem() {
        for (Elevator elevator : elevators) {
            new Thread(elevator).start();
        }
        System.out.println("Elevator system started with " + elevators.size() + " elevators");
    }

    public void requestElevator(ExternalRequest request) {
        Elevator bestElevator = findBestElevator(request);
        bestElevator.addRequest(request);
        System.out.println("External request from floor " + request.getTargetFloor() + 
                         " (" + request.getDirection() + ") assigned to " + bestElevator);
    }

    public void sendInternalRequest(InternalRequest request, Elevator elevator) {
        elevator.addRequest(request);
    }

    private Elevator findBestElevator(ExternalRequest request) {
        return elevators.stream()
                .min((e1, e2) -> Integer.compare(
                    Math.abs(e1.getCurrentFloor() - request.getTargetFloor()),
                    Math.abs(e2.getCurrentFloor() - request.getTargetFloor())
                )).orElse(elevators.get(0));
    }

    public void shutdown() {
        for (Elevator elevator : elevators) {
            elevator.shutdown();
        }
    }
}

public class Main {
    public static void main(String[] args) throws InterruptedException {
        Elevator elevatorA = new Elevator("A");
        Elevator elevatorB = new Elevator("B");
        
        ElevatorSystem system = new ElevatorSystem(List.of(elevatorA, elevatorB));
        
        system.requestElevator(new ExternalRequest(3, ExternalRequest.Direction.UP));
        system.requestElevator(new ExternalRequest(5, ExternalRequest.Direction.DOWN));
        
        Thread.sleep(2500);
        system.sendInternalRequest(new InternalRequest(7), elevatorA);
        
        Thread.sleep(15000);
        system.shutdown();
    }
}
