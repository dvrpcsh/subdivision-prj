package com.subdivision.subdivision_prj.domain.specification;

import com.subdivision.subdivision_prj.domain.Pot;
import com.subdivision.subdivision_prj.domain.PotCategory;
import com.subdivision.subdivision_prj.domain.PotStatus;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

@Component
public class PotSpecification {

    /**
     * 키워드(제목, 내용, 상품명) 검색을 위한 Specification
     */
    public static Specification<Pot> likeKeyword(String keyword) {
        //(root, query, criteriaBuilder) -> Predicate
        return (root, query, cb) -> {
            //제목(title) 또는 내용(content) 또는 상품명(productName)에 키워드가 포함되어 있는지 확인
            return cb.or(
                    cb.like(root.get("title"), "%" + keyword + "%"),
                    cb.like(root.get("content"), "%" + keyword + "%"),
                    cb.like(root.get("productName"), "%" + keyword + "%")
            );
        };
    }

    /**
     * 카테고리 필터링을 위한 Specification
     */
    public static Specification<Pot> equalCategory(PotCategory category) {
        return (root, query, cb) -> cb.equal(root.get("category"), category);
    }

    /**
     * 상태 필터링을 위한 Specification
     */
    public static Specification<Pot> equalStatus(PotStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }
}
