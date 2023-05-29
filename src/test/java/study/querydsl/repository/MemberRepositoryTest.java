package study.querydsl.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class MemberRepositoryTest {
    @Autowired
    EntityManager em;

    @Autowired
    MemberRepository memberRepository;

    @Test
    void basicTest() throws Exception{
        // given
        Member member =new Member("member1", 10);

        // when
        memberRepository.save(member);

        Member findMember = memberRepository.findById(member.getId()).get();
        List<Member> result1 = memberRepository.findAll();
        List<Member> result2 = memberRepository.findByUsername("member1");

        // then
        assertThat(findMember).isEqualTo(member);
        assertThat(result1).containsExactly(member);
        assertThat(result2).containsExactly(member);
    }

    @Test
    void searchTest() throws Exception{
        // given
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);

        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);
        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);

        MemberSearchCondition condition = new MemberSearchCondition();
//        condition.setAgeGoe(35);
//        condition.setAgeLoe(40);
//        condition.setTeamName("teamB");

        PageRequest pageRequest = PageRequest.of(0, 3);
        // when
        Page<MemberTeamDto> result = memberRepository.searchPageSimple(condition, pageRequest);
//        List<MemberTeamDto> result = memberRepository.search(condition);

        for (MemberTeamDto memberTeamDto : result) {
            System.out.println("result.getContent() = " + result.getContent());
            System.out.println("result.getSize() = " + result.getSize());
        }

        // then
//        assertThat(result).extracting("username").containsExactly("member4");
        assertThat(result.getSize()).isEqualTo(3);
        assertThat(result.getContent()).extracting("username").containsExactly("member1", "member2", "member3");

    }

    @Test
    void querydslPredicateExecutorTest() throws Exception{
        // given
        Member member1 = new Member("member1", 10);
        Member member2 = new Member("member2", 20);

        Member member3 = new Member("member3", 30);
        Member member4 = new Member("member4", 40);
        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);

        QMember member = QMember.member;
        // when
        Iterable<Member> result = memberRepository.findAll(member.age.between(0, 101).and(member.username.eq("member1")));
        List<Member> all = memberRepository.findAll();
        // then
//        for (Member findMember : result) {
            System.out.println("result = " + result);
            System.out.println("all = " + all);
//        }

    }


}