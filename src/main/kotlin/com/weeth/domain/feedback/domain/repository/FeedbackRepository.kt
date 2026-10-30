package com.weeth.domain.feedback.domain.repository

import com.weeth.domain.feedback.domain.entity.Feedback
import org.springframework.data.jpa.repository.JpaRepository

interface FeedbackRepository : JpaRepository<Feedback, Long>
