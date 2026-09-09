package workshop.setup;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;
import jakarta.jms.ConnectionFactory;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.UserTransaction;
import org.apache.camel.component.jms.JmsComponent;
import org.springframework.transaction.jta.JtaTransactionManager;

// Supplied plumbing: start XA BEFORE receiving, then commit SQL + acknowledgement together.
@ApplicationScoped
public class JmsSetup {
    @Produces
    @Named("jms")
    @ApplicationScoped
    JmsComponent jms(ConnectionFactory connections, TransactionManager manager, UserTransaction transactions) {
        var jta = new JtaTransactionManager(transactions, manager);
        var jms = JmsComponent.jmsComponent(connections);
        jms.setTransactionManager(jta); // XA owns commit; do not enable a second local JMS transaction.
        jms.setCacheLevelName("CACHE_NONE");
        jms.setConcurrentConsumers(1);
        return jms;
    }
}
