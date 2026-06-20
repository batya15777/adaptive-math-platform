package com.adaptive.server.repository;

import com.adaptive.server.entity.QuestionArchive;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Set;

public interface QuestionArchiveRepository extends JpaRepository<QuestionArchive, Long> {
//היא שולפת רק את השדה expression (הביטוי המתמטי של השאלה, למשל "5 + 3") עבור תלמיד ספציפי בנושא ספציפי.
 //במקום לשלוף את כל האובייקט QuestionArchive מה-DB (שכולל פתרונות, אפשרויות, קושי וכו'),
 // אתם שולפים רק String (הביטוי). זה הופך את השאילתה למהירה מאוד וחסכונית בזיכרון,
 // גם אם לתלמיד יש היסטוריה של מאות שאלות.

    @Query("SELECT a.expression FROM QuestionArchive a " +
           "WHERE a.user.id = :userId AND a.subSubject.id = :subSubjectId")
    Set<String> findSeenExpressionsByUserAndSubSubject(
            @Param("userId") Long userId,
            @Param("subSubjectId") Long subSubjectId);
}
