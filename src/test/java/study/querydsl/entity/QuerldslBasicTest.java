package study.querydsl.entity;

import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;
import java.util.List;

import static com.querydsl.jpa.JPAExpressions.select;
import static org.assertj.core.api.Assertions.assertThat;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

@SpringBootTest
@Transactional
class QuerldslBasicTest {

    @Autowired
    EntityManager em;

    JPAQueryFactory queryFactory;

    //    QMember qMember = QMember.member;
    @PersistenceUnit
    EntityManagerFactory emf;

    @BeforeEach
    void before() {
        queryFactory = new JPAQueryFactory(em);

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
    }

    @Test
    void startJQPL() throws Exception {
        // when
        // member1을 찾아라.
        Member findMember = em.createQuery(
                        "select m from Member m where m.username = :username"
                        , Member.class)
                .setParameter("username", "member1")
                .getSingleResult();


        // then
        assertThat(findMember.getUsername()).isEqualTo("member1");

    }

    @Test
    void startQuerydsl() throws Exception {
        // when
        Member findMember = queryFactory
                .select(member)
                .from(member)
                .where(member.username.eq("member1"))  // 파라미터 바인딩
                .fetchFirst();

        // then
        assertThat(findMember.getUsername()).isEqualTo("member1");

    }

    @Test
    void search() throws Exception {
        // given
        String param = "1";
        // when
        Member findMember = queryFactory
                .selectFrom(member)
                .where(
                        member.username.eq("member1")
                                .and(member.age.eq(10))
//                        qMember.username.contains(param)
//                                .or(qMember.age.like("%"+param+"%"))
//                                .or(qMember.age.stringValue().contains(param))    // 숫자를 스트링으로 변환하면 contatins사용가능
                )
                .fetchFirst();
        System.out.println("findMember.toString() = " + findMember.toString());
        // then
        assertThat(findMember.getUsername()).isEqualTo("member1");

    }

    @Test
    void resultFetchTest() throws Exception {
        // when
        List<Member> fetch = queryFactory
                .selectFrom(member)
                .fetch();

        Member fetchOne = queryFactory
                .selectFrom(member)
                .fetchOne();

        Member fetchFirst = queryFactory
                .selectFrom(member)
                .fetchFirst();

        // 쿼리 두번실행 (count, content (추후삭제))
//        QueryResults<Member> results = queryFactory
//                .selectFrom(member)
//                .fetchResults();
//
//        results.getTotal();
//        List<Member> content = results.getResults();

        // count 쿼리만 실행 (추후삭제)
//        long fetchCount = queryFactory
//                .selectFrom(member)
//                .fetchCount();

    }

    /**
     * 회원 정렬 순서
     * 1. 회원 나이 내림차순(desc)
     * 2. 회원 이름 올림차순(asc)
     * 단 2에서 회원 이름이 없으면 마지막에 출력(nulls last)
     */
    @Test
    void sort() throws Exception {
        // given
        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));

        // when
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();

        // then
        Member member5 = result.get(0);
        Member member6 = result.get(1);
        Member memberNull = result.get(2);

        assertThat(member5.getUsername()).isEqualTo("member5");
        assertThat(member6.getUsername()).isEqualTo("member6");
        assertThat(memberNull.getUsername()).isNull();
    }

    @Test
    void paging1() throws Exception {
        // when
        List<Member> result = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetch();

        // then
        assertThat(result.size()).isEqualTo(2);

    }

    /**
     * 전체조회 정렬
     */
    @Test
    void paging2() throws Exception {
        // when
        QueryResults<Member> queryResults = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetchResults();

        // then
        System.out.println("queryResults.getTotal() = " + queryResults.getTotal());
        System.out.println("queryResults.getLimit() = " + queryResults.getLimit());
        System.out.println("queryResults.getOffset() = " + queryResults.getOffset());
        System.out.println("queryResults.getResults() = " + queryResults.getResults());
        assertThat(queryResults.getTotal()).isEqualTo(4);
        assertThat(queryResults.getLimit()).isEqualTo(2);
        assertThat(queryResults.getOffset()).isEqualTo(1);
        assertThat(queryResults.getResults().size()).isEqualTo(2);

    }

    @Test
    void aggregation() throws Exception {
        // when
        List<Tuple> list = queryFactory
                .select(
                        member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min()
                )
                .from(member)
                .fetch();

        Tuple tuple = list.get(0);

        // then
        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        assertThat(tuple.get(member.age.avg())).isEqualTo(25);
        assertThat(tuple.get(member.age.max())).isEqualTo(40);
        assertThat(tuple.get(member.age.min())).isEqualTo(10);

    }

    /**
     * 팀이름과 각 팀의 평균연령을 구해라
     */
    @Test
    void QuerldslBasicAvgTest() throws Exception {
        // when
        List<Tuple> result = queryFactory
                .select(
                        team.name,
                        member.age.avg()
                )
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .fetch();

        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);

        // then
        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15);

        assertThat(teamB.get(team.name)).isEqualTo("teamB");
        assertThat(teamB.get(member.age.avg())).isEqualTo(35);

    }

    /**
     * 팀A에 소속된 모든 회원 찾기
     */
    @Test
    void join() throws Exception {
        // when
        List<Member> result = queryFactory
                .selectFrom(member)
                .join(member.team, team)
//                .leftJoin(qMember.team, team)
                .where(team.name.eq("teamA"))
                .fetch();

        System.out.println("result = " + result.toString());
        // then
        assertThat(result)
                .extracting("username")
                .containsExactly("member1", "member2");

    }

    /**
     * 세타 조인
     * 회원의 이름이 팀 이름과 같은 회원 조회
     * 외부조인(left, outer join)이 불가능 > 조인 on을 사용하면 가능
     * (중복테이터가 나온 후 where로 걸러냄)
     */
    @Test
    void theta_join() throws Exception {
        // given
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        // when
        List<Member> result = queryFactory
                .select(member)
                .from(member, team)
                .where(member.username.eq(team.name))
                .fetch();

        // then
        assertThat(result)
                .extracting("username")
                .containsExactly("teamA", "teamB");

    }

    /**
     * 회원과 팀을 조인후 팀 이름이 teamA인 팀만 조인, 회원은 모두 조회
     * JPQL: select m, t from Member m left join m.team t on t.name = 'teamA'
     * (leftJoin에선 on절이 의미가 있지만 inner에선 where절이랑 같음)
     */
    @Test
    void join_on_filtering() throws Exception {
        // when
        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
//                .leftJoin(member.team, team)
                .join(member.team, team)
                .on(team.name.eq("teamA"))
                .fetch();

        // then
        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }

    }

    /**
     * 연관관계가 없는 엔티티 외부 조인
     * 회원의 이름이 팀 이름과 같은 대상 외부 조인
     */
    @Test
    void join_on_no_relation() throws Exception {
        // given
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        // when
        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(team)
                .on(member.username.eq(team.name))
//                .where(member.username.eq(team.name))
//                .where(team.name.isNotNull())
                .fetch();

        // then
        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    @Test
    void fetchJoinNo() throws Exception {
        // given
        // test땐 fetchJoin일 때 영속성컨택스트를 안지워주면 결과보기 어려움
        em.flush();
        em.clear();

        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();
        // when


        // then
        // JUnit5에서 예외처리는 이런식으로 함(try,catch가능)
        IllegalStateException thrown = Assertions.assertThrows(IllegalStateException.class, () -> {
            // em에서 로딩을 했는지 알려주는 코드
            boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());

            assertThat(loaded).as("패치 조인 미적용").isFalse();
        });
    }

    @Test
    void fetchJoinUse() throws Exception {
        // given
        // test땐 fetchJoin일 때 영속성컨택스트를 안지워주면 결과보기 어려움
        em.flush();
        em.clear();

        Member findMember = queryFactory
                .selectFrom(member)
                .join(member.team, team)
                .fetchJoin()
                .where(member.username.eq("member1"))
                .fetchOne();
        // when
        // JUnit5에서 예외처리는 이런식으로 함(try,catch가능)
        IllegalStateException thrown = Assertions.assertThrows(IllegalStateException.class, () -> {
            // em에서 로딩을 했는지 알려주는 코드
            boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());

            // then
            System.out.println("findMember = " + findMember);
            assertThat(loaded).as("패치 조인 미적용").isTrue();
        });
    }

    /**
     * 나이가 가장 많은 회원 조회
     */
    @Test
    void subQuery() throws Exception {
        // given
        // 서브쿼리 사용시 같은 테이블이 중첩되면 안되기 때문에 생성
        QMember memberSub = new QMember("memberSub");

        // when
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(
                        member.age.eq(
                                select(memberSub.age.max())
                                        .from(memberSub)
                        )
                )
                .fetch();

        // then
        assertThat(result).extracting("age").containsExactly(40);

    }

    /**
     * 나이가 평균이상 회원 조회
     */
    @Test
    void subQueryGoe() throws Exception {
        // given
        // 서브쿼리 사용시 같은 테이블이 중첩되면 안되기 때문에 생성
        QMember memberSub = new QMember("memberSub");

        // when
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(
                        member.age.goe(
                                select(memberSub.age.avg())
                                        .from(memberSub)
                        )
                )
                .fetch();

        // then
        System.out.println("result = " + result);
        assertThat(result).extracting("age").containsExactly(30, 40);

    }

    /**
     * 나이가 평균이상 회원 조회
     */
    @Test
    void subQueryIn() throws Exception {
        // given
        // 서브쿼리 사용시 같은 테이블이 중첩되면 안되기 때문에 생성
        QMember memberSub = new QMember("memberSub");

        // when
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(
                        member.age.in(
                                select(memberSub.age)
                                        .from(memberSub)
                                        .where(memberSub.age.gt(10))
                        )
                )
                .fetch();

        // then
        System.out.println("result = " + result);
        assertThat(result).extracting("age").containsExactly(20, 30, 40);

    }

    @Test
    void selectSubquery() throws Exception {
        // given
        QMember memberSub = new QMember("memberSub");

        // when
        List<Tuple> result = queryFactory
                .select(
                        member.username,
                        select(memberSub.age.avg())
                                .from(memberSub)
                )
                .from(member)
                .fetch();

        // then
        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }

    }

    @Test
    void basicCase() throws Exception {
        // when
        List<String> result = queryFactory
                .select(
//                        member.age,
                        member.age
                                .when(10).then("열살")
                                .when(20).then("스무살")
                                .otherwise("기타")
                )
                .from(member)
                .fetch();

        // then
        for (String s : result) {
            System.out.println("s = " + s);
        }

    }

    @Test
    void coplexCase() throws Exception {
        // when
        List<String> result = queryFactory
                .select(new CaseBuilder()
                        .when(member.age.between(0, 20)).then("0~20살")
                        .when(member.age.between(21, 30)).then("21~30살")
                        .otherwise("기타")
                )
                .from(member)
                .fetch();

        // then
        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    void constant() throws Exception {
        // when
        List<Tuple> result = queryFactory
                .select(member.username
                        // 쿼리엔 안나가고 결과에서만 상수값을 받음
                        , Expressions.constant("A")
                )
                .from(member)
                .fetch();

        // then
        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    @Test
    void concat() throws Exception {
        // when
        List<String> result = queryFactory
                .select(
                        // {(String)username}_{(int)age} 타입이 달라 concat이 안됨 .stringValue로 문자열로 변환시켜줘야함
                        member.username.concat("_").concat(member.age.stringValue())
                )
                .from(member)
                .where(member.username.eq("member1"))
                .fetch();

        // then
        for (String s : result) {
            System.out.println("s = " + s);
        }

    }
}
