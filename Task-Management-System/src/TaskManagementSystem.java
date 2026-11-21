import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Scanner;

public class TaskManagementSystem {

    // ArrayList stores all tasks.
    // Queue uses FIFO: the oldest pending task is processed first.
    // Stack uses LIFO: the most recent action is undone first.
    static final ArrayList<Task> tasks = new ArrayList<>();
    static final Deque<Task> taskQueue = new ArrayDeque<>();
    static final Deque<Action> undoStack = new ArrayDeque<>();
    static final Scanner scanner = new Scanner(System.in);
    static int nextId = 1;

    static class Task {
        int id;
        String title;
        String description;
        LocalDate deadline;
        int priority; // 1 = High, 2 = Medium, 3 = Low
        boolean completed;

        Task(int id, String title, String description,
             LocalDate deadline, int priority) {
            this.id = id;
            this.title = title;
            this.description = description;
            this.deadline = deadline;
            this.priority = priority;
            this.completed = false;
        }
    }

    enum ActionType {
        ADD, COMPLETE, DELETE
    }

    static class Action {
        ActionType type;
        Task task;
        int originalIndex;

        Action(ActionType type, Task task, int originalIndex) {
            this.type = type;
            this.task = task;
            this.originalIndex = originalIndex;
        }
    }

    public static void main(String[] args) {
        int choice;

        do {
            System.out.println("\n======= TASK MANAGEMENT SYSTEM =======");
            System.out.println("1. Add Task");
            System.out.println("2. View All Tasks");
            System.out.println("3. Process Next Task (Queue)");
            System.out.println("4. Complete Task by ID");
            System.out.println("5. Delete Task");
            System.out.println("6. Undo Last Action (Stack)");
            System.out.println("7. Search Task (Linear Search)");
            System.out.println("8. Sort by Deadline (Bubble Sort)");
            System.out.println("9. Sort by Priority (Bubble Sort)");
            System.out.println("10. View Pending Queue");
            System.out.println("0. Exit");

            choice = readInt("Choose an option: ");

            switch (choice) {
                case 1: addTask(); break;
                case 2: displayTasks(tasks); break;
                case 3: processNextTask(); break;
                case 4: completeTaskById(); break;
                case 5: deleteTask(); break;
                case 6: undoLastAction(); break;
                case 7: searchTask(); break;
                case 8: showSortedTasks(true); break;
                case 9: showSortedTasks(false); break;
                case 10: viewQueue(); break;
                case 0: System.out.println("Goodbye!"); break;
                default: System.out.println("Invalid menu option.");
            }
        } while (choice != 0);

        scanner.close();
    }

    static void addTask() {
        String title = readRequiredText("Task name: ");
        System.out.print("Description (optional): ");
        String description = scanner.nextLine().trim();
        LocalDate deadline = readDate("Deadline (YYYY-MM-DD): ");
        int priority = readPriority();

        Task task = new Task(nextId++, title, description, deadline, priority);
        tasks.add(task);
        undoStack.push(new Action(ActionType.ADD, task, -1));
        rebuildQueue();
        System.out.println("Task added successfully! ID: " + task.id);
    }

    static void processNextTask() {
        // FIFO: peek() takes the first pending task in the queue.
        Task task = taskQueue.peek();
        if (task == null) {
            System.out.println("No pending tasks in the queue.");
            return;
        }

        markCompleted(task);
        System.out.println("Processed and completed: " + task.title);
    }

    static void completeTaskById() {
        int id = readInt("Enter task ID to complete: ");
        Task task = findById(id); // Linear search
        if (task == null) {
            System.out.println("Task not found.");
        } else if (task.completed) {
            System.out.println("This task is already completed.");
        } else {
            markCompleted(task);
            System.out.println("Completed: " + task.title);
        }
    }

    static void markCompleted(Task task) {
        task.completed = true;
        undoStack.push(new Action(ActionType.COMPLETE, task, -1));
        rebuildQueue();
    }

    static void deleteTask() {
        int id = readInt("Enter task ID to delete: ");
        for (int i = 0; i < tasks.size(); i++) {
            if (tasks.get(i).id == id) {
                Task removed = tasks.remove(i);
                undoStack.push(new Action(ActionType.DELETE, removed, i));
                rebuildQueue();
                System.out.println("Deleted: " + removed.title);
                return;
            }
        }
        System.out.println("Task not found.");
    }

    static void undoLastAction() {
        // LIFO: pop() undoes the most recent recorded change first.
        if (undoStack.isEmpty()) {
            System.out.println("Nothing to undo.");
            return;
        }

        Action action = undoStack.pop();
        switch (action.type) {
            case ADD:
                tasks.remove(action.task);
                System.out.println("Undo successful: added task removed.");
                break;
            case COMPLETE:
                action.task.completed = false;
                System.out.println("Undo successful: task is pending again.");
                break;
            case DELETE:
                tasks.add(action.originalIndex, action.task);
                System.out.println("Undo successful: deleted task restored.");
                break;
        }
        rebuildQueue();
    }

    static void searchTask() {
        System.out.println("1. Search by ID");
        System.out.println("2. Search by task name");
        int option = readInt("Choose search type: ");
        ArrayList<Task> matches = new ArrayList<>();

        if (option == 1) {
            int id = readInt("Enter ID: ");
            // Linear search: examine each element until an ID matches.
            for (Task task : tasks) {
                if (task.id == id) {
                    matches.add(task);
                    break;
                }
            }
        } else if (option == 2) {
            String keyword = readRequiredText("Enter task name or keyword: ").toLowerCase();
            // Linear search: examine each task's title one by one.
            for (Task task : tasks) {
                if (task.title.toLowerCase().contains(keyword)) {
                    matches.add(task);
                }
            }
        } else {
            System.out.println("Invalid search type.");
            return;
        }

        if (matches.isEmpty()) {
            System.out.println("No matching tasks found.");
        } else {
            displayTasks(matches);
        }
    }

    static Task findById(int id) {
        for (Task task : tasks) { // Linear Search
            if (task.id == id) return task;
        }
        return null;
    }

    static void showSortedTasks(boolean byDeadline) {
        // Sort a COPY, so sorting never changes FIFO queue order.
        ArrayList<Task> sorted = new ArrayList<>(tasks);

        // Bubble Sort: compare adjacent tasks and swap if out of order.
        for (int i = 0; i < sorted.size() - 1; i++) {
            boolean swapped = false;
            for (int j = 0; j < sorted.size() - 1 - i; j++) {
                Task a = sorted.get(j);
                Task b = sorted.get(j + 1);
                int comparison;

                if (byDeadline) {
                    comparison = a.deadline.compareTo(b.deadline);
                } else {
                    comparison = Integer.compare(a.priority, b.priority);
                }

                if (comparison > 0) {
                    sorted.set(j, b);
                    sorted.set(j + 1, a);
                    swapped = true;
                }
            }
            if (!swapped) break;
        }

        System.out.println(byDeadline
                ? "\nTasks sorted by earliest deadline:"
                : "\nTasks sorted by priority (High to Low):");
        displayTasks(sorted);
    }

    static void viewQueue() {
        if (taskQueue.isEmpty()) {
            System.out.println("The pending queue is empty.");
            return;
        }
        System.out.println("\nPending Queue (FIRST IN -> FIRST OUT):");
        int position = 1;
        for (Task task : taskQueue) {
            System.out.println(position++ + ". [ID " + task.id + "] " + task.title);
        }
    }

    static void rebuildQueue() {
        // Preserve original task-creation order when refreshing pending tasks.
        taskQueue.clear();
        for (Task task : tasks) {
            if (!task.completed) {
                taskQueue.offer(task); // Enqueue at the back.
            }
        }
    }

    static void displayTasks(List<Task> list) {
        if (list.isEmpty()) {
            System.out.println("No tasks to display.");
            return;
        }
        System.out.println("\nID    TASK NAME                DEADLINE     PRIORITY  STATUS");
        System.out.println("----------------------------------------------------------------");
        for (Task task : list) {
            String priority = task.priority == 1 ? "High"
                    : task.priority == 2 ? "Medium" : "Low";
            String status = task.completed ? "Completed" : "Pending";
            System.out.printf("%-5d %-24.24s %-12s %-9s %s%n",
                    task.id, task.title, task.deadline, priority, status);
            if (!task.description.isEmpty()) {
                System.out.println("      Description: " + task.description);
            }
        }
    }

    static int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                return Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid whole number.");
            }
        }
    }

    static String readRequiredText(String prompt) {
        while (true) {
            System.out.print(prompt);
            String value = scanner.nextLine().trim();
            if (!value.isEmpty()) return value;
            System.out.println("This field cannot be empty.");
        }
    }

    static LocalDate readDate(String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                return LocalDate.parse(scanner.nextLine().trim());
            } catch (DateTimeParseException e) {
                System.out.println("Invalid date. Use YYYY-MM-DD (example: 2026-11-05).");
            }
        }
    }

    static int readPriority() {
        while (true) {
            int priority = readInt("Priority (1=High, 2=Medium, 3=Low): ");
            if (priority >= 1 && priority <= 3) return priority;
            System.out.println("Priority must be 1, 2, or 3.");
        }
    }
}
