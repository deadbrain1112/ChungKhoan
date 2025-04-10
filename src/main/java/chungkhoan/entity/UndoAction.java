package chungkhoan.entity;

public class UndoAction {
    public enum ActionType { ADD, DELETE, EDIT }

    private ActionType actionType;
    private NhaDauTu oldData;
    private NhaDauTu newData;

    public UndoAction(ActionType actionType, NhaDauTu oldData, NhaDauTu newData) {
        this.actionType = actionType;
        this.oldData = oldData;
        this.newData = newData;
    }

    public ActionType getActionType() {
        return actionType;
    }

    public NhaDauTu getOldData() {
        return oldData;
    }

    public NhaDauTu getNewData() {
        return newData;
    }
}
