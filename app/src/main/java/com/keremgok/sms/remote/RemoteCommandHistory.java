package com.keremgok.sms.remote;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Entity representing a remote SMS command execution history
 * Logs all received commands (authorized and unauthorized)
 */
@Entity(
    tableName = "remote_command_history",
    indices = {
        @Index(value = "received_timestamp"),
        @Index(value = "sender_number")
    }
)
public class RemoteCommandHistory {
    
    /**
     * Command execution status
     */
    public enum Status {
        PENDING,        // Command received, waiting for validation
        AUTHORIZED,     // Command authorized, waiting for execution
        EXECUTING,      // Command currently being executed
        SUCCESS,        // Command executed successfully
        FAILED,         // Command execution failed
        UNAUTHORIZED,   // Command rejected - sender not authorized
        INVALID_FORMAT, // Command rejected - invalid format
        RATE_LIMITED,   // Command rejected - rate limit exceeded
        USER_DENIED     // Command rejected - user denied confirmation
    }
    
    @PrimaryKey(autoGenerate = true)
    private int id;
    
    @ColumnInfo(name = "sender_number")
    private String senderNumber;
    
    @ColumnInfo(name = "command_text")
    private String commandText;
    
    @ColumnInfo(name = "parsed_sim")
    private String parsedSim;
    
    @ColumnInfo(name = "parsed_target")
    private String parsedTarget;
    
    @ColumnInfo(name = "parsed_message")
    private String parsedMessage;
    
    @ColumnInfo(name = "execution_status")
    private String executionStatus;
    
    @ColumnInfo(name = "result_message")
    private String resultMessage;
    
    @ColumnInfo(name = "received_timestamp")
    private long receivedTimestamp;
    
    @ColumnInfo(name = "executed_timestamp")
    private Long executedTimestamp;
    
    @ColumnInfo(name = "result_timestamp")
    private Long resultTimestamp;
    
    /**
     * Constructor for initial command receipt
     */
    public RemoteCommandHistory(String senderNumber, String commandText, long receivedTimestamp) {
        this.senderNumber = senderNumber;
        this.commandText = commandText;
        this.receivedTimestamp = receivedTimestamp;
        this.executionStatus = Status.PENDING.name();
    }
    
    /**
     * Create a new command history entry
     */
    public static RemoteCommandHistory create(String senderNumber, String commandText) {
        return new RemoteCommandHistory(senderNumber, commandText, System.currentTimeMillis());
    }
    
    /**
     * Mark command as authorized
     */
    public void markAuthorized(String sim, String target, String message) {
        this.parsedSim = sim;
        this.parsedTarget = target;
        this.parsedMessage = message;
        this.executionStatus = Status.AUTHORIZED.name();
    }
    
    /**
     * Mark command as executing
     */
    public void markExecuting() {
        this.executionStatus = Status.EXECUTING.name();
        this.executedTimestamp = System.currentTimeMillis();
    }
    
    /**
     * Mark command as successful
     */
    public void markSuccess(String resultMessage) {
        this.executionStatus = Status.SUCCESS.name();
        this.resultMessage = resultMessage;
        this.resultTimestamp = System.currentTimeMillis();
    }
    
    /**
     * Mark command as failed
     */
    public void markFailed(String errorMessage) {
        this.executionStatus = Status.FAILED.name();
        this.resultMessage = errorMessage;
        this.resultTimestamp = System.currentTimeMillis();
    }
    
    /**
     * Mark command as unauthorized
     */
    public void markUnauthorized(String reason) {
        this.executionStatus = Status.UNAUTHORIZED.name();
        this.resultMessage = reason;
        this.resultTimestamp = System.currentTimeMillis();
    }
    
    /**
     * Mark command as invalid format
     */
    public void markInvalidFormat(String reason) {
        this.executionStatus = Status.INVALID_FORMAT.name();
        this.resultMessage = reason;
        this.resultTimestamp = System.currentTimeMillis();
    }
    
    /**
     * Mark command as rate limited
     */
    public void markRateLimited(String reason) {
        this.executionStatus = Status.RATE_LIMITED.name();
        this.resultMessage = reason;
        this.resultTimestamp = System.currentTimeMillis();
    }
    
    /**
     * Mark command as user denied
     */
    public void markUserDenied() {
        this.executionStatus = Status.USER_DENIED.name();
        this.resultMessage = "User denied confirmation";
        this.resultTimestamp = System.currentTimeMillis();
    }
    
    /**
     * Check if command was successful
     */
    public boolean isSuccess() {
        return Status.SUCCESS.name().equals(executionStatus);
    }
    
    /**
     * Check if command is pending or executing
     */
    public boolean isInProgress() {
        return Status.PENDING.name().equals(executionStatus) ||
               Status.AUTHORIZED.name().equals(executionStatus) ||
               Status.EXECUTING.name().equals(executionStatus);
    }
    
    // Getters and Setters
    
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public String getSenderNumber() {
        return senderNumber;
    }
    
    public void setSenderNumber(String senderNumber) {
        this.senderNumber = senderNumber;
    }
    
    public String getCommandText() {
        return commandText;
    }
    
    public void setCommandText(String commandText) {
        this.commandText = commandText;
    }
    
    public String getParsedSim() {
        return parsedSim;
    }
    
    public void setParsedSim(String parsedSim) {
        this.parsedSim = parsedSim;
    }
    
    public String getParsedTarget() {
        return parsedTarget;
    }
    
    public void setParsedTarget(String parsedTarget) {
        this.parsedTarget = parsedTarget;
    }
    
    public String getParsedMessage() {
        return parsedMessage;
    }
    
    public void setParsedMessage(String parsedMessage) {
        this.parsedMessage = parsedMessage;
    }
    
    public String getExecutionStatus() {
        return executionStatus;
    }
    
    public void setExecutionStatus(String executionStatus) {
        this.executionStatus = executionStatus;
    }
    
    public String getResultMessage() {
        return resultMessage;
    }
    
    public void setResultMessage(String resultMessage) {
        this.resultMessage = resultMessage;
    }
    
    public long getReceivedTimestamp() {
        return receivedTimestamp;
    }
    
    public void setReceivedTimestamp(long receivedTimestamp) {
        this.receivedTimestamp = receivedTimestamp;
    }
    
    public Long getExecutedTimestamp() {
        return executedTimestamp;
    }
    
    public void setExecutedTimestamp(Long executedTimestamp) {
        this.executedTimestamp = executedTimestamp;
    }
    
    public Long getResultTimestamp() {
        return resultTimestamp;
    }
    
    public void setResultTimestamp(Long resultTimestamp) {
        this.resultTimestamp = resultTimestamp;
    }
    
    @Override
    public String toString() {
        return "RemoteCommandHistory{" +
                "id=" + id +
                ", senderNumber='" + senderNumber + '\'' +
                ", executionStatus='" + executionStatus + '\'' +
                ", resultMessage='" + resultMessage + '\'' +
                '}';
    }
}
