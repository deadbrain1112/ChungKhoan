package chungkhoan.repository;

import jakarta.persistence.*;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public class SaoKeRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public List<Object[]> callSaoKeGiaoDich(String maNDT,
                                            String maTK,
                                            String maCP,
                                            LocalDateTime fromDate,
                                            LocalDateTime toDate) {
        StoredProcedureQuery query = entityManager
                .createStoredProcedureQuery("sp_SaoKeGiaoDich");

        query.registerStoredProcedureParameter("MaNDT", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("MaTK", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("MaCP", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("FromDate", LocalDateTime.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("ToDate", LocalDateTime.class, ParameterMode.IN);

        query.setParameter("MaNDT", maNDT);
        query.setParameter("MaTK", maTK);
        query.setParameter("MaCP", maCP); // null được chấp nhận
        query.setParameter("FromDate", fromDate);
        query.setParameter("ToDate", toDate);

        return query.getResultList();
    }
}
