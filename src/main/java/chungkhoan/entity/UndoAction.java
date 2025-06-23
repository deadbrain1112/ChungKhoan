package chungkhoan.entity;

import lombok.Getter;

@Getter
public class UndoAction {

    public enum ActionType { ADD, DELETE, EDIT }
    public enum EntityType { NHA_DAU_TU, CO_PHIEU, NHAN_VIEN ,NGAN_HANG}

    private final ActionType actionType;
    private final EntityType entityType;

    private final Object oldData;
    private final Object newData;

    public UndoAction(ActionType actionType, EntityType entityType, Object oldData, Object newData) {
        this.actionType = actionType;
        this.entityType = entityType;
        this.oldData = oldData;
        this.newData = newData;
    }

    public NhaDauTu getOldNhaDauTu() {
        return entityType == EntityType.NHA_DAU_TU ? (NhaDauTu) oldData : null;
    }

    public CoPhieu getOldCoPhieu() {
        return entityType == EntityType.CO_PHIEU ? (CoPhieu) oldData : null;
    }

    public NhanVien getOldNhanVien() {
        return entityType == EntityType.NHAN_VIEN ? (NhanVien) oldData : null;
    }

}