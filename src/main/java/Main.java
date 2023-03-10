import com.fasterxml.jackson.databind.ObjectMapper;
import dao.CityDAO;
import dao.CountryDAO;
import dataProcessor.MyRedisClient;
import dataProcessor.MySessionFactory;
import dataProcessor.Processor;
import domain.City;
import io.lettuce.core.RedisClient;
import org.hibernate.SessionFactory;
import redis.CityCountry;
import java.util.List;
import static java.util.Objects.nonNull;

public class Main {
    private final SessionFactory sessionFactory;
    private final RedisClient redisClient;

    private final ObjectMapper mapper;

    private final CityDAO cityDAO;
    private final CountryDAO countryDAO;

    MyRedisClient myRedisClient = new MyRedisClient();

    public Main() {
        sessionFactory = MySessionFactory.getSessionFactory();
        cityDAO = new CityDAO(sessionFactory);
        countryDAO = new CountryDAO(sessionFactory);

        redisClient = myRedisClient.prepareRedisClient();
        mapper = new ObjectMapper();
    }

    private void shutdown() {
        if (nonNull(sessionFactory)) {
            sessionFactory.close();
        }
        if (nonNull(redisClient)) {
            redisClient.shutdown();
        }
    }

    public static void main(String[] args) {
        Main main = new Main();
        Processor processor = new Processor();
        List<City> allCities = processor.fetchData(main.sessionFactory, main.cityDAO);
        List<CityCountry> preparedData = processor.transformData(allCities);
        processor.pushToRedis(preparedData, main.redisClient,main.mapper);

        main.sessionFactory.getCurrentSession().close();

        List<Integer> ids = List.of(3, 2545, 123, 4, 189, 89, 3458, 1189, 10, 102, 22);

        long startRedis = System.currentTimeMillis();
        processor.testRedisData(ids,main.redisClient, main.mapper);
        long stopRedis = System.currentTimeMillis();

        long startMySQL = System.currentTimeMillis();
        processor.testMysqlData(ids,main.sessionFactory, main.cityDAO);
        long stopMySQL = System.currentTimeMillis();

        System.out.printf("%s:\t%d ms\n", "Redis", (stopRedis-startRedis));
        System.out.printf("%s:\t%d ms\n", "MySQL", (stopMySQL-startMySQL));

        main.shutdown();
    }
}
