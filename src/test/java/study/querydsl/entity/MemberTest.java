package study.querydsl.entity;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.util.List;
@SpringBootTest
@Transactional
class MemberTest {

    @Autowired
    EntityManager em;

    @Test
    void MemberHasTest() throws Exception{
        // given
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 20, teamA);
        Member member2 = new Member("member2", 30, teamA);

        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);
        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);

        // 초기화
        em.flush(); // 쿼리를 날리고
        em.clear(); // 영속성 context를 초기화 함

        // when
        List<Member> members = em.createQuery("select m from Member m", Member.class).getResultList();

        // then
        for (Member member : members) {
            System.out.println("member = " + member);
            System.out.println("Team = " + member.getTeam());
        }
    }
}