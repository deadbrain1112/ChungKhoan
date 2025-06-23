package chungkhoan.service;

import chungkhoan.entity.CoPhieu;
import chungkhoan.entity.UndoAction;
import chungkhoan.repository.CoPhieuRepository;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CoPhieuService {

    @Autowired
    private CoPhieuRepository coPhieuRepository;
    
    @Autowired 
    private LichSuGiaService lichSuGiaService;
    
    private Deque<UndoAction> undoStack = new ArrayDeque<>();

    public Page<CoPhieu> getPaginated(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("maCP").ascending());
        return coPhieuRepository.findAll(pageable);
    }

    // Thêm mới sử dụng stored procedure
    public void themCoPhieuBangSP(CoPhieu stock) {
        coPhieuRepository.themCoPhieu(
                stock.getMaCP(),
                stock.getTenCty(),
                stock.getDiaChi(),
                stock.getSoLuongPH()
        );
        undoStack.push(new UndoAction(
                UndoAction.ActionType.ADD,
                UndoAction.EntityType.CO_PHIEU,
                null,
                stock
        ));
    }

    // Xóa cổ phiếu
    public void xoaCoPhieu(String maCP) {
        CoPhieu cp = coPhieuRepository.findById(maCP).orElse(null);
        if (cp != null) {
            coPhieuRepository.deleteById(maCP);
            undoStack.push(new UndoAction(
                UndoAction.ActionType.DELETE,
                UndoAction.EntityType.CO_PHIEU,
                cp,
                null
            ));
        }
    }

    // Cập nhật cổ phiếu
    public void capNhatCoPhieu(String maCP, CoPhieu cpMoi) {
        CoPhieu cpCu = coPhieuRepository.findById(maCP).orElse(null);
        if (cpCu != null) {
            CoPhieu copy = new CoPhieu(cpCu);

            cpMoi.setMaCP(maCP);

            coPhieuRepository.save(cpMoi);
            undoStack.push(new UndoAction(
                UndoAction.ActionType.EDIT,
                UndoAction.EntityType.CO_PHIEU,
                copy,
                cpMoi
            ));
        }
    }
    
    // Tìm cổ phiếu theo ID
    public Optional<CoPhieu> findById(String maCP) {
        return coPhieuRepository.findById(maCP);
    }

    // Lấy tất cả cổ phiểu
    public List<CoPhieu> getAllCoPhieu() {
    	return coPhieuRepository.findAll();
    }

    //
    public List<CoPhieu> findByMaCPIn(List<String> maCPs) {
        return coPhieuRepository.findByMaCPIn(maCPs);
    }

    @Transactional
    public boolean undoThaoTacCuoi() {
        if (undoStack.isEmpty()) return false;

        UndoAction action = undoStack.pop();
        if (action.getEntityType() != UndoAction.EntityType.CO_PHIEU) return false;

        CoPhieu oldCP = (CoPhieu) action.getOldData();
        CoPhieu newCP = (CoPhieu) action.getNewData();

        switch (action.getActionType()) {
            case ADD:
                coPhieuRepository.deleteById(newCP.getMaCP());
                break;
            case DELETE:
                coPhieuRepository.save(oldCP);
                break;
            case EDIT:
                coPhieuRepository.save(oldCP);
                break;
        }

        return true;
    }

    // Kiểm tra stack rỗng
    public boolean isUndoStackEmpty() {
    	return undoStack.isEmpty();
    }

    public void clearUndoStack() {
        undoStack.clear();
    }

    public double layGiaMoiNhat(String maCP) {
        return lichSuGiaService.layGiaMoiNhat(maCP).getGiaTC();
    }

    public boolean existsById(String maCP) {
        return coPhieuRepository.existsByMaCP(maCP);
    }
}