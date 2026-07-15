package model.user;

/**
 * Transient state of the registration and password-recovery flows.
 *
 * <p>A registration that has not answered its security question yet lives here,
 * not in the roster: no half-configured account is ever stored.</p>
 */
public class AuthFlowState {

    private User pendingRegistration;
    private User recoveringUser;
    private boolean recoveryAnswerAccepted;

    public User getPendingRegistration() {
        return pendingRegistration;
    }

    public void setPendingRegistration(User pendingRegistration) {
        this.pendingRegistration = pendingRegistration;
    }

    public User getRecoveringUser() {
        return recoveringUser;
    }

    public boolean isRecoveryAnswerAccepted() {
        return recoveryAnswerAccepted;
    }

    public void startRecovery(User user) {
        this.recoveringUser = user;
        this.recoveryAnswerAccepted = false;
    }

    public void acceptRecoveryAnswer() {
        this.recoveryAnswerAccepted = true;
    }

    public void clearRecovery() {
        this.recoveringUser = null;
        this.recoveryAnswerAccepted = false;
    }

    public void clear() {
        this.pendingRegistration = null;
        clearRecovery();
    }
}
