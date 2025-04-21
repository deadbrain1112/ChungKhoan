package chungkhoan.entity;

import lombok.Getter;

@Getter
public class UndoAction {
    public enum ActionType { ADD, DELETE, EDIT }

    private ActionType actionType;
    
    private NhaDauTu oldData;
    private NhaDauTu newData;
    
    private CoPhieu cpOldData;
    private CoPhieu cpNewData;
    
    private NhanVien nvOldData;
    private NhanVien nvNewData;

    public UndoAction(ActionType actionType, NhaDauTu oldData, NhaDauTu newData) {
        this.actionType = actionType;
        this.oldData = oldData;
        this.newData = newData;
    }
    
    public UndoAction(ActionType actionType, CoPhieu cpOldData, CoPhieu cpNewData) {
        this.actionType = actionType;
        this.cpOldData = cpOldData;
        this.cpNewData = cpNewData;
    }
    
    public UndoAction(ActionType actionType, NhanVien nvOldData, NhanVien nvNewData) {
    	this.actionType = actionType;
        this.nvOldData = nvOldData;
        this.nvNewData = nvNewData;
    }
}
