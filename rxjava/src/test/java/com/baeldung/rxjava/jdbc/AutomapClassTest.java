package com.baeldung.rxjava.jdbc;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.github.davidmoten.rx.jdbc.ConnectionProvider;
import com.github.davidmoten.rx.jdbc.ConnectionProviderFromUrl;
import com.github.davidmoten.rx.jdbc.Database;

import rx.Observable;

public class AutomapClassTest {

    private String DB_CONNECTION = Connector.DB_CONNECTION;
    private String DB_USER = Connector.DB_USER;
    private String DB_PASSWORD = Connector.DB_PASSWORD;

    ConnectionProvider cp = null;
    Database db = null;

    Observable<Integer> create = null;
    Observable<Integer> insert1, insert2 = null;

    @Before
    public void setup() {
        cp = new ConnectionProviderFromUrl(DB_CONNECTION, DB_USER, DB_PASSWORD);
        db = Database.from(cp);

        create = db.update("CREATE TABLE IF NOT EXISTS MANAGER(id int primary key, name varchar(255))")
            .count();
        insert1 = db.update("INSERT INTO MANAGER(id, name) VALUES(1, 'Alan')")
            .dependsOn(create)
            .count();
        insert2 = db.update("INSERT INTO MANAGER(id, name) VALUES(2, 'Sarah')")
            .dependsOn(create)
            .count();
    }

    @Test
    public void whenSelectManagersAndAutomap_thenCorrect() {
        List<Manager> managers = db.select("select id, name from MANAGER")
            .dependsOn(create)
            .dependsOn(insert1)
            .dependsOn(insert2)
            .autoMap(Manager.class)
            .toList()
            .toBlocking()
            .single();

        assertThat(managers.get(0)
            .getId()).isEqualTo(1);
        assertThat(managers.get(0)
            .getName()).isEqualTo("Alan");
        assertThat(managers.get(1)
            .getId()).isEqualTo(2);
        assertThat(managers.get(1)
            .getName()).isEqualTo("Sarah");
    }

    @After
    public void close() {
        db.update("DROP TABLE MANAGER")
            .dependsOn(create);
        cp.close();
    }
}