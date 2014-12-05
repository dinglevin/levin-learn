package org.levin.protobuf.reorg.simple;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectStreamException;

import com.google.protobuf.AbstractParser;
import com.google.protobuf.ByteString;
import com.google.protobuf.CodedInputStream;
import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FileDescriptor;
import com.google.protobuf.ExtensionRegistry;
import com.google.protobuf.ExtensionRegistryLite;
import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.GeneratedMessage.FieldAccessorTable;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.Parser;
import com.google.protobuf.UnknownFieldSet;

public final class SearchRequestProtos {
    private SearchRequestProtos() { }
    
    private static Descriptor internal_static_levin_protobuf_SearchRequest_descriptor;
    private static FieldAccessorTable internal_static_levin_protobuf_SearchRequest_fieldAccessorTable;
    
    public static FileDescriptor descriptor;
    
    public static FileDescriptor getDescriptor() {
        return descriptor;
    }
    
    public static void registerAllExtensions(ExtensionRegistry registry) { 
        // TODO what to implement??
    }
    
    static {
        String[] descriptorData = {
                "\n\031SimpleSearchRequest.proto\022\016levin.proto" +
                "buf\"W\n\rSearchRequest\022\024\n\014query_string\030\001 \002" +
                "(\t\022\023\n\013page_number\030\002 \001(\005\022\033\n\017result_per_pa" +
                "ge\030\003 \001(\005:\00250B:\n#org.levin.protobuf.reorg" +
                ".simpleB\023SearchRequestProtos"
              };
        
        FileDescriptor.InternalDescriptorAssigner assigner = new FileDescriptor.InternalDescriptorAssigner() {
            public ExtensionRegistry assignDescriptors(FileDescriptor root) {
                descriptor = root;
                internal_static_levin_protobuf_SearchRequest_descriptor = getDescriptor().getMessageTypes().get(0);
                internal_static_levin_protobuf_SearchRequest_fieldAccessorTable = new FieldAccessorTable(
                        internal_static_levin_protobuf_SearchRequest_descriptor, 
                        new String[] { "QueryString", "PageNumber", "ResultPerPage" });
                return null;
            }
        };
        
        FileDescriptor.internalBuildGeneratedFileFrom(descriptorData, new FileDescriptor[] { }, assigner);
    }
    
    public interface SearchRequestOrBuilder extends MessageOrBuilder {
        boolean hasQueryString();
        String getQueryString();
        ByteString getQueryStringBytes();
        
        boolean hasPageNumber();
        int getPageNumber();
        
        boolean hasResultPerPage();
        int getResultPerPage();
    }
    
    public static final class SearchRequest extends GeneratedMessage implements SearchRequestOrBuilder {
        private static final long serialVersionUID = 0L;
        
        private static final SearchRequest defaultInstance;
        
        public static final int QUERY_STRING_FIELD_NUMBER = 1;
        public static final int PAGE_NUMBER_FIELD_NUMBER = 2;
        public static final int RESULT_PER_PAGE_FIELD_NUMBER = 3;
        
        public static final Parser<SearchRequest> PARSER = new AbstractParser<SearchRequest>() {
            public SearchRequest parsePartialFrom(CodedInputStream input, ExtensionRegistryLite extensionRegistry)
                    throws InvalidProtocolBufferException {
                return new SearchRequest(input, extensionRegistry);
            }
        };
        
        public static SearchRequest getDefaultInstance() {
            return defaultInstance;
        }
        
        public static final Descriptor getDescriptor() {
            return SearchRequestProtos.internal_static_levin_protobuf_SearchRequest_descriptor;
        }
        
        static {
            defaultInstance = new SearchRequest(true);
            defaultInstance.initFields();
        }
        
        private int bitField0_;
        private Object queryString_;
        private int pageNumber_;
        private int resultPerPage_;
        
        private byte memoizedIsInitialized = -1;
        private int memoizedSerializedSize = -1;
        
        private final UnknownFieldSet unknownFields;
        
        private SearchRequest(Builder builder) {
            super(builder);
            unknownFields = builder.getUnknownFields();
        }
        
        private SearchRequest(boolean noInit) {
            this.unknownFields = UnknownFieldSet.getDefaultInstance();
        }
        
        private SearchRequest(CodedInputStream input, ExtensionRegistryLite extensionRegistry)
            throws InvalidProtocolBufferException {
            initFields();
            
            UnknownFieldSet.Builder unknownFields = UnknownFieldSet.newBuilder();
            try {
                boolean done = false;
                while (!done) {
                    int tag = input.readTag();
                    switch (tag) {
                        case 0:
                            done = true;
                            break;
                        case 10: {
                            bitField0_ |= 0x00000001;
                            queryString_ = input.readBytes();
                            break;
                        }
                        case 16: {
                            bitField0_ |= 0x00000002;
                            pageNumber_ = input.readInt32();
                            break;
                        }
                        case 24: {
                            bitField0_ |= 0x00000004;
                            resultPerPage_ = input.readInt32();
                            break;
                        }
                        default: {
                            if (!parseUnknownField(input, unknownFields, extensionRegistry, tag)) {
                                done = true;
                            }
                            break;
                        }
                    }
                }
            } catch (InvalidProtocolBufferException e) {
                throw e.setUnfinishedMessage(this);
            } catch (IOException e) {
                throw new InvalidProtocolBufferException(e.getMessage()).setUnfinishedMessage(this);
            } finally {
                this.unknownFields = unknownFields.build();
                makeExtensionsImmutable();
            }
        }
        
        public final UnknownFieldSet getUnknownFieldSet() {
            return unknownFields;
        }
        
        public boolean hasQueryString() {
            return ((bitField0_ & 0x00000001) == 0x00000001);
        }

        public String getQueryString() {
            Object ref = queryString_;
            if (ref instanceof String) {
                return (String) ref;
            } else {
                ByteString bs = (ByteString) ref;
                String s = bs.toStringUtf8();
                if (bs.isValidUtf8()) {
                    queryString_ = s;
                }
                return s;
            }
        }

        public ByteString getQueryStringBytes() {
            Object ref = queryString_;
            if (ref instanceof ByteString) {
                return (ByteString) ref;
            } else {
                String s = (String) ref;
                ByteString bs = ByteString.copyFromUtf8(s);
                queryString_ = bs;
                return bs;
            }
        }

        public boolean hasPageNumber() {
            return ((bitField0_ & 0x00000002) == 0x00000002);
        }

        public int getPageNumber() {
            return pageNumber_;
        }

        public boolean hasResultPerPage() {
            return ((bitField0_ & 0x00000004) == 0x00000004);
        }

        public int getResultPerPage() {
            return resultPerPage_;
        }
        
        @Override
        public final boolean isInitialized() {
            byte isInitialized = memoizedIsInitialized;
            if (isInitialized != -1) return isInitialized == 1;
            
            if (!hasQueryString()) {
                memoizedIsInitialized = 0;
                return false;
            }
            
            memoizedIsInitialized = 1;
            return true;
        }
        
        @Override
        public int getSerializedSize() {
            int size = memoizedSerializedSize;
            if (size != -1) return size;
            
            size = 0;
            if (hasQueryString()) {
                size += CodedOutputStream.computeBytesSize(1, getQueryStringBytes());
            }
            if (hasPageNumber()) {
                size += CodedOutputStream.computeInt32Size(2, pageNumber_);
            }
            if (hasResultPerPage()) {
                size += CodedOutputStream.computeInt32Size(3, resultPerPage_);
            }
            size += getUnknownFields().getSerializedSize();
            memoizedSerializedSize = size;
            return size;
        }
        
        @Override
        public void writeTo(CodedOutputStream output) throws IOException {
            getSerializedSize();
            if (hasQueryString()) {
                output.writeBytes(1, getQueryStringBytes());
            }
            if (hasPageNumber()) {
                output.writeInt32(2, pageNumber_);
            }
            if (hasResultPerPage()) {
                output.writeInt32(3, resultPerPage_);
            }
            getUnknownFields().writeTo(output);
        }

        @Override
        public Parser<SearchRequest> getParserForType() {
            return PARSER;
        }
        
        public SearchRequest getDefaultInstanceForType() {
            return defaultInstance;
        }

        public Builder newBuilderForType() {
            return newBuilder();
        }

        public Builder toBuilder() {
            return newBuilder(this);
        }
        
        @Override
        protected Builder newBuilderForType(BuilderParent parent) {
            return new Builder(parent);
        }
        
        public static Builder newBuilder() { return Builder.create(); }
        public static Builder newBuilder(SearchRequest prototype) {
            return newBuilder().mergeFrom(prototype);
        }
        
        public static SearchRequest parseFrom(ByteString data) throws InvalidProtocolBufferException {
            return PARSER.parseFrom(data);
        }
        public static SearchRequest parseFrom(ByteString data, ExtensionRegistryLite extensionRegistry)
            throws InvalidProtocolBufferException {
          return PARSER.parseFrom(data, extensionRegistry);
        }
        public static SearchRequest parseFrom(byte[] data) throws InvalidProtocolBufferException {
          return PARSER.parseFrom(data);
        }
        public static SearchRequest parseFrom(byte[] data, ExtensionRegistryLite extensionRegistry)
            throws InvalidProtocolBufferException {
          return PARSER.parseFrom(data, extensionRegistry);
        }
        public static SearchRequest parseFrom(java.io.InputStream input) throws IOException {
          return PARSER.parseFrom(input);
        }
        public static SearchRequest parseFrom(InputStream input, ExtensionRegistryLite extensionRegistry) throws IOException {
          return PARSER.parseFrom(input, extensionRegistry);
        }
        public static SearchRequest parseDelimitedFrom(InputStream input) throws IOException {
          return PARSER.parseDelimitedFrom(input);
        }
        public static SearchRequest parseDelimitedFrom(InputStream input, ExtensionRegistryLite extensionRegistry) throws IOException {
          return PARSER.parseDelimitedFrom(input, extensionRegistry);
        }
        public static SearchRequest parseFrom(CodedInputStream input) throws IOException {
          return PARSER.parseFrom(input);
        }
        public static SearchRequest parseFrom(CodedInputStream input, ExtensionRegistryLite extensionRegistry) throws IOException {
          return PARSER.parseFrom(input, extensionRegistry);
        }

        @Override
        protected FieldAccessorTable internalGetFieldAccessorTable() {
            return internal_static_levin_protobuf_SearchRequest_fieldAccessorTable.ensureFieldAccessorsInitialized(
                    SearchRequest.class, SearchRequest.Builder.class);
        }
        
        // For Java serialization only
        @Override
        protected Object writeReplace() throws ObjectStreamException {
            return super.writeReplace();
        }
        
        private void initFields() {
            queryString_ = "";
            pageNumber_ = 0;
            resultPerPage_ = 50;
        }
        
        public static final class Builder extends GeneratedMessage.Builder<Builder> 
                implements SearchRequestOrBuilder {
            
            private int bitField0_;
            private Object queryString_ = "";
            private int pageNumber_;
            private int resultPerPage_ = 50;
            
            public static final Descriptor getDescriptor() {
                return internal_static_levin_protobuf_SearchRequest_descriptor;
            }
            
            private static Builder create() { return new Builder(); }
            
            private Builder() {
                maybeForceBuilderInitialization();
            }
            
            private Builder(BuilderParent parent) {
                super(parent);
                maybeForceBuilderInitialization();
            }
            
            private void maybeForceBuilderInitialization() {
                if (GeneratedMessage.alwaysUseFieldBuilders) {
                }
            }
            
            public boolean hasQueryString() {
                return ((bitField0_ & 0x00000001) == 0x00000001);
            }

            public String getQueryString() {
                Object ref = queryString_;
                if (ref instanceof String) {
                    return (String) ref;
                } else {
                    ByteString bs = (ByteString) queryString_;
                    String s = bs.toStringUtf8();
                    queryString_ = s;
                    return s;
                }
            }

            public ByteString getQueryStringBytes() {
                Object ref = queryString_;
                if (ref instanceof ByteString) {
                    return (ByteString) ref;
                } else {
                    String s = (String) ref;
                    ByteString bs = ByteString.copyFromUtf8(s);
                    queryString_ = bs;
                    return bs;
                }
            }
            
            public Builder setQueryString(String value) {
                if (value == null) {
                    throw new NullPointerException();
                }
                
                bitField0_ |= 0x00000001;
                queryString_ = value;
                onChanged();
                return this;
            }
            
            public Builder clearQueryString() {
                bitField0_ = (bitField0_ & ~0x00000001);
                queryString_ = getDefaultInstance().getQueryString();
                onChanged();
                return this;
            }
            
            public Builder setQueryStringBytes(ByteString value) {
                if (value == null) {
                    throw new NullPointerException();
                }
                
                bitField0_ |= 0x00000001;
                queryString_ = value;
                onChanged();
                return this;
            }

            public boolean hasPageNumber() {
                return ((bitField0_ & 0x00000002) == 0x00000002);
            }

            public int getPageNumber() {
                return pageNumber_;
            }
            
            public Builder setPageNumber(int value) {
                bitField0_ |= 0x00000002;
                pageNumber_ = value;
                onChanged();
                return this;
            }
            
            public Builder clearPageNumber() {
                bitField0_ = (bitField0_ & ~0x00000002);
                pageNumber_ = getDefaultInstance().getPageNumber();
                onChanged();
                return this;
            }

            public boolean hasResultPerPage() {
                return ((bitField0_ & 0x00000004) == 0x00000004);
            }

            public int getResultPerPage() {
                return resultPerPage_;
            }
            
            public Builder setResultPerPage(int value) {
                bitField0_ |= 0x00000004;
                resultPerPage_ = value;
                onChanged();
                return this;
            }
            
            public Builder clearResultPerPage() {
                bitField0_ = (bitField0_ & ~0x00000004);
                resultPerPage_ = getDefaultInstance().getResultPerPage();
                onChanged();
                return this;
            }
            
            @Override
            public Builder mergeFrom(CodedInputStream input, ExtensionRegistryLite extensionRegistry) throws IOException {
                SearchRequest parsedMessage = null;
                try {
                    parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
                } catch (InvalidProtocolBufferException e) {
                    parsedMessage = (SearchRequest) e.getUnfinishedMessage();
                    throw e;
                } finally {
                    if (parsedMessage != null) {
                        mergeFrom(parsedMessage);
                    }
                }
                return this;
            }
            
            @Override
            public Builder mergeFrom(Message other) {
                if (other instanceof SearchRequest) {
                    return mergeFrom((SearchRequest) other);
                } else {
                    super.mergeFrom(other);
                    return this;
                }
            }
            
            public Builder mergeFrom(SearchRequest other) {
                if (other == getDefaultInstance()) return this;
                if (other.hasQueryString()) {
                    setQueryString(other.getQueryString());
                }
                if (other.hasPageNumber()) {
                    setPageNumber(other.getPageNumber());
                }
                if (other.hasResultPerPage()) {
                    setResultPerPage(other.getResultPerPage());
                }
                mergeUnknownFields(other.getUnknownFields());
                return this;
            }

            public SearchRequest build() {
                SearchRequest result = buildPartial();
                if (!result.isInitialized()) {
                    throw newUninitializedMessageException(result);
                }
                return result;
            }

            public SearchRequest buildPartial() {
                SearchRequest result = new SearchRequest(this);
                
                int fromBitField = bitField0_;
                int toBitField = 0;
                
                if ((fromBitField & 0x00000001) == 0x00000001) {
                    toBitField |= 0x00000001;
                }
                result.queryString_ = queryString_;
                if ((fromBitField & 0x00000002) == 0x00000002) {
                    toBitField |= 0x00000002;
                }
                result.pageNumber_ = pageNumber_;
                if ((fromBitField & 0x00000004) == 0x00000004) {
                    toBitField |= 0x00000004;
                }
                result.resultPerPage_ = resultPerPage_;
                result.bitField0_ = toBitField;
                onBuilt();
                return result;
            }
            
            @Override
            public final boolean isInitialized() {
                if (!hasQueryString()) {
                    return false;
                }
                return true;
            }
            
            public Message getDefaultInstanceForType() {
                return getDefaultInstanceForType();
            }
            
            public Builder clone() {
                return create().mergeFrom(buildPartial());
            }
            
            @Override
            public Builder clear() {
                super.clear();
                queryString_ = "";
                bitField0_ = (bitField0_ & ~0x00000001);
                pageNumber_ = 0;
                bitField0_ = (bitField0_ & ~0x00000002);
                resultPerPage_ = 50;
                bitField0_ = (bitField0_ & ~0x00000004);
                return this;
            }
            
            @Override
            public Descriptor getDescriptorForType() {
                return internal_static_levin_protobuf_SearchRequest_descriptor;
            }

            @Override
            protected FieldAccessorTable internalGetFieldAccessorTable() {
                return internal_static_levin_protobuf_SearchRequest_fieldAccessorTable.ensureFieldAccessorsInitialized(
                        SearchRequest.class, Builder.class);
            }
            
        }
    }
}
