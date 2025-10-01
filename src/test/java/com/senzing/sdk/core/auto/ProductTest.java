package com.senzing.sdk.core.auto;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import com.senzing.sdk.SzProduct;
import com.senzing.sdk.core.auto.SzAutoCoreEnvironment;
import com.senzing.sdk.SzException;
import com.senzing.sdk.test.SzProductTest;

@TestInstance(Lifecycle.PER_CLASS)
@Execution(ExecutionMode.SAME_THREAD)
public class ProductTest 
    extends AbstractAutoCoreTest 
    implements SzProductTest 
{
    private SzAutoCoreEnvironment env = null;

    /**
     * @inheritDoc
     */
    public SzProduct getProduct() throws SzException {
        return this.env.getProduct();
    }

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
}
