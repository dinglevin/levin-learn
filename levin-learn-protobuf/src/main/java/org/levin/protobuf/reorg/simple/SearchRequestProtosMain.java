package org.levin.protobuf.reorg.simple;

import java.util.Arrays;

import org.levin.protobuf.generated.simple.SearchRequestProtos.SearchRequest;
import org.levin.protobuf.generated.simple.SearchRequestProtos.Test;

public class SearchRequestProtosMain {
    public static void main(String[] args) throws Exception {
        SearchRequest.Builder builder = SearchRequest.newBuilder();
        builder.setQueryString("param1=value1&param2=value2");
        builder.setPageNumber(10);
        builder.setResultPerPage(100);
        
        SearchRequest request = builder.build();
        System.out.println(request);
        System.out.println(Arrays.toString(request.toByteArray()));
        
        System.out.println(SearchRequest.newBuilder().mergeFrom(request.toByteArray()).build());
        
        Test test = Test.newBuilder().setTest2(-10).build();
        System.out.println(test);
        System.out.println("field: " + test.getTest2());
    }
}
