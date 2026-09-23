package com.derekgelvez.logbooked.repository;

import com.derekgelvez.logbooked.model.Entry;
import com.derekgelvez.logbooked.model.ReadingSession;
import com.derekgelvez.logbooked.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ReadingSessionRepository extends JpaRepository<ReadingSession, Long> {

    Optional<ReadingSession> findTopByEntryOrderByDateDesc(Entry entry);

    List<ReadingSession> findByEntryOrderByDateAsc(Entry entry);

    List<ReadingSession> findByEntry_UserAndDateBetween(User user, LocalDate start, LocalDate end);

}
