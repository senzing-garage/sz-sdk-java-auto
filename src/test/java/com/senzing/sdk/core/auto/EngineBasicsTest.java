package com.senzing.sdk.core.auto;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import com.senzing.sdk.SzEngine;
import com.senzing.sdk.SzException;
import com.senzing.sdk.core.auto.SzAutoCoreEnvironment;
import com.senzing.sdk.test.SzEngineBasicsTest;

import static org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import static org.junit.jupiter.api.TestInstance.Lifecycle;

/**
 * Unit tests for {@link SzCoreDiagnostic}.
 */
@TestInstance(Lifecycle.PER_CLASS)
@Execution(ExecutionMode.SAME_THREAD)
@TestMethodOrder(OrderAnnotation.class)
public class EngineBasicsTest 
    extends AbstractAutoCoreTest 
    implements SzEngineBasicsTest
{

    private SzAutoCoreEnvironment env = null;

    @BeforeAll
    public void initializeEnvironment() {
        this.beginTests();
        this.initializeTestEnvironment();
        String settings = this.getRepoSettings();
        
        String instanceName = this.getClass().getSimpleName();
        
        this.env = SzAutoCoreEnvironment.newAutoBuilder()
                                             .instanceName(instanceName)
                                             .settings(settings)
                                             .verboseLogging(false)
                                             .concurrency(this.getConcurrency())
                                             .configRefreshPeriod(this.getConfigRefreshPeriod())
                                             .build();
    }
    
    @AfterAll
    public void teardownEnvironment() {
        try {
            if (this.env != null) {
                this.env.destroy();
                this.env = null;
            }
            this.teardownTestEnvironment();
        } finally {
            this.endTests();
        }
    }

    /**
     * Gets the {@link SzEngine} from the {@link SzCoreEnvironment}.
     * {@inheritDoc}
     * 
     * @return The {@link SzEngine} to use for this test.
     */
    public SzEngine getEngine() throws SzException {
        return this.env.getEngine();
    }
}
