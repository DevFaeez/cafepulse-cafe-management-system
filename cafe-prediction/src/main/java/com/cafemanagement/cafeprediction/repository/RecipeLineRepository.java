package com.cafemanagement.cafeprediction.repository;

import com.cafemanagement.cafeprediction.model.RecipeLine;
import com.cafemanagement.cafeprediction.model.RecipeLine.Owner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface RecipeLineRepository extends JpaRepository<RecipeLine, Long> {
    List<RecipeLine> findByOwnerTypeAndOwnerIdOrderById(Owner ownerType, long ownerId);
    List<RecipeLine> findByOwnerTypeAndOwnerIdIn(Owner ownerType, Collection<Long> owners);

    @Modifying
    @Query("delete from RecipeLine r where r.ownerType = :type and r.ownerId = :id")
    void removeRecipe(@Param("type") Owner type, @Param("id") long id);
}
