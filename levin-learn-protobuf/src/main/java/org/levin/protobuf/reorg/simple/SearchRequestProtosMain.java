package org.levin.protobuf.reorg.simple;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;

import org.levin.protobuf.generated.simple.SearchRequestProtos.SearchRequest;

public class SearchRequestProtosMain {
    public static void main(String[] args) throws Exception {
        SearchRequest.Builder builder = SearchRequest.newBuilder();
        builder.setQueryString("param1=value1&param2=value2");
        builder.setPageNumber(10);
        builder.setResultPerPage(100);
        
        SearchRequest request = builder.build();
        System.out.println(request);
        
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        request.writeTo(bytes);
        System.out.println(Arrays.toString(bytes.toByteArray()));
        //output: [10, 27, 112, 97, 114, 97, 109, 49, 61, 118, 97, 108, 117, 101, 49, 38, 112, 97, 114, 97, 109, 50, 61, 118, 97, 108, 117, 101, 50, 16, 10, 24, 100]
        
        System.out.println(SearchRequest.newBuilder().mergeFrom(request.toByteArray()).build());
        //output: 
        // query_string: "param1=value1&param2=value2"
        // page_number: 10
        // result_per_page: 100
    }
}
