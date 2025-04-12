package chungkhoan.entity;

import lombok.Getter;

@Getter
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

}
