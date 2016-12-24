/**
 * Copyright (C) 2013-2016 Vasilis Vryniotis <bbriniotis@datumbox.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.datumbox.framework.core.machinelearning.clustering;

import com.datumbox.framework.common.Configuration;
import com.datumbox.framework.common.dataobjects.Dataframe;
import com.datumbox.framework.core.machinelearning.MLBuilder;
import com.datumbox.framework.core.machinelearning.datatransformation.DummyXYMinMaxNormalizer;
import com.datumbox.framework.core.machinelearning.modelselection.metrics.ClusteringMetrics;
import com.datumbox.framework.core.machinelearning.modelselection.Validator;
import com.datumbox.framework.core.machinelearning.modelselection.splitters.KFoldSplitter;
import com.datumbox.framework.tests.Constants;
import com.datumbox.framework.tests.Datasets;
import com.datumbox.framework.tests.abstracts.AbstractTest;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Test cases for HierarchicalAgglomerative.
 *
 * @author Vasilis Vryniotis <bbriniotis@datumbox.com>
 */
public class HierarchicalAgglomerativeTest extends AbstractTest {

    /**
     * Test of predict method, of class HierarchicalAgglomerative.
     */
    @Test
    public void testPredict() {
        logger.info("testPredict");
        
        Configuration conf = Configuration.getConfiguration();
        
        
        Dataframe[] data = Datasets.heartDiseaseClusters(conf);
        
        Dataframe trainingData = data[0];
        Dataframe validationData = data[1];
        
        
        String dbName = this.getClass().getSimpleName();
        DummyXYMinMaxNormalizer df = MLBuilder.create(new DummyXYMinMaxNormalizer.TrainingParameters(), conf);
        
        df.fit_transform(trainingData);
        df.save(dbName);

        
        HierarchicalAgglomerative.TrainingParameters param = new HierarchicalAgglomerative.TrainingParameters();
        param.setDistanceMethod(HierarchicalAgglomerative.TrainingParameters.Distance.EUCLIDIAN);
        param.setLinkageMethod(HierarchicalAgglomerative.TrainingParameters.Linkage.COMPLETE);
        param.setMinClustersThreshold(2);
        param.setMaxDistanceThreshold(Double.MAX_VALUE);

        HierarchicalAgglomerative instance = MLBuilder.create(param, conf);
        instance.fit(trainingData);
        instance.save(dbName);

        df.denormalize(trainingData);
        trainingData.close();
        
        instance.close();
        df.close();
        //instance = null;
        //df = null;
        
        df = MLBuilder.load(DummyXYMinMaxNormalizer.class, dbName, conf);
        instance = MLBuilder.load(HierarchicalAgglomerative.class, dbName, conf);

        df.transform(validationData);
        instance.predict(validationData);
        ClusteringMetrics vm = new ClusteringMetrics(validationData);

        df.denormalize(validationData);

        double expResult = 1.0;
        double result = vm.getPurity();
        assertEquals(expResult, result, Constants.DOUBLE_ACCURACY_HIGH);
        
        df.delete();
        instance.delete();

        validationData.close();
    }

    
    /**
     * Test of validate method, of class HierarchicalAgglomerative.
     */
    @Test
    public void testKFoldCrossValidation() {
        logger.info("testKFoldCrossValidation");
        
        Configuration conf = Configuration.getConfiguration();
        
        int k = 5;
        
        Dataframe[] data = Datasets.heartDiseaseClusters(conf);
        Dataframe trainingData = data[0];
        data[1].close();
        

        DummyXYMinMaxNormalizer df = MLBuilder.create(new DummyXYMinMaxNormalizer.TrainingParameters(), conf);
        df.fit_transform(trainingData);

        
        

        
        HierarchicalAgglomerative.TrainingParameters param = new HierarchicalAgglomerative.TrainingParameters();
        param.setDistanceMethod(HierarchicalAgglomerative.TrainingParameters.Distance.EUCLIDIAN);
        param.setLinkageMethod(HierarchicalAgglomerative.TrainingParameters.Linkage.COMPLETE);
        param.setMinClustersThreshold(2);
        param.setMaxDistanceThreshold(Double.MAX_VALUE);

        ClusteringMetrics vm = new Validator<>(ClusteringMetrics.class, conf)
                .validate(new KFoldSplitter(k).split(trainingData), param);

        df.denormalize(trainingData);

        
        double expResult = 0.7666666666666667;
        double result = vm.getPurity();
        assertEquals(expResult, result, Constants.DOUBLE_ACCURACY_HIGH);
        
        df.close();
        
        trainingData.close();
    }

}
