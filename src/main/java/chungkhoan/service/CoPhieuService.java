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
    
    private Deque<UndoAction> undoStack = new ArrayDeque<>();

    public Page<CoPhieu> getPaginated(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("maCP").ascending());
        return coPhieuRepository.findAll(pageable);
    }

    // Ghi hoặc cập nhật
    public CoPhieu save(CoPhieu stock) {
        return coPhieuRepository.save(stock);
    }

    // Thêm mới sử dụng stored procedure
    public void themCoPhieuBangSP(CoPhieu stock) {
        coPhieuRepository.themCoPhieu(
                stock.getMaCP(),
                stock.getTenCty(),
                stock.getDiaChi(),
                stock.getSoLuongPH()
        );
    }

    public Optional<CoPhieu> findById(String maCP) {
        return coPhieuRepository.findById(maCP);
    }

    public void deleteById(String maCP) {
        coPhieuRepository.deleteById(maCP);
    }
    
    public List<CoPhieu> getAllCoPhieu() {
    	return coPhieuRepository.findAll();
    }
    
    public List<CoPhieu> findByMaCPIn(List<String> maCPs) {
        return coPhieuRepository.findByMaCPIn(maCPs);
    }
    
    // Hoàn tác
    @Transactional
    public boolean undoThaoTacCuoi() {
        if (undoStack.isEmpty()) return false;

        UndoAction action = undoStack.pop();

        switch (action.getActionType()) {
            case ADD:
            	coPhieuRepository.deleteById(action.getCpNewData().getMaCP());
                break;
            case DELETE:
            	coPhieuRepository.save(action.getCpOldData());
                break;
            case EDIT:
            	coPhieuRepository.save(action.getCpOldData());
                break;
        }

        return true;
    }
    
    // Kiểm tra stack rỗng
    public boolean isUndoStackEmpty() {
    	return undoStack.isEmpty();
    }
}
