CREATE   TABLE region  ( R_REGIONKEY  INTEGER NOT NULL,
                            R_NAME       CHAR(25) NOT NULL,
                            R_COMMENT    VARCHAR(152),
                            primary key (R_REGIONKEY)                           
                            );
                            
CREATE   TABLE nation  ( N_NATIONKEY  INTEGER NOT NULL,
                            N_NAME       CHAR(25) NOT NULL,
                            N_REGIONKEY  INTEGER NOT NULL,
                            N_COMMENT    VARCHAR(152),
                            primary key (N_NATIONKEY),
                             FOREIGN KEY (N_REGIONKEY) REFERENCES REGION (R_REGIONKEY)                             
                            );


CREATE   TABLE part  ( P_PARTKEY     INTEGER NOT NULL,
                          P_NAME        VARCHAR(55) NOT NULL,
                          P_MFGR        CHAR(25) NOT NULL,
                          P_BRAND       CHAR(10) NOT NULL,
                          P_TYPE        VARCHAR(25) NOT NULL,
                          P_SIZE        INTEGER NOT NULL,
                          P_CONTAINER   CHAR(10) NOT NULL,
                          P_RETAILPRICE numeric(15,2) NOT NULL,
                          P_COMMENT     VARCHAR(23) NOT NULL ,
                          PRIMARY KEY (P_PARTKEY)
                          );

CREATE   TABLE supplier ( S_SUPPKEY     INTEGER NOT NULL,
                             S_NAME        CHAR(25) NOT NULL,
                             S_ADDRESS     VARCHAR(40) NOT NULL,
                             S_NATIONKEY   INTEGER NOT NULL,
                             S_PHONE       CHAR(15) NOT NULL,
                             S_ACCTBAL     numeric(15,2) NOT NULL,
                             S_COMMENT     VARCHAR(101) NOT NULL,
                             PRIMARY KEY (S_SUPPKEY),
                             FOREIGN KEY (S_NATIONKEY) REFERENCES NATION (N_NATIONKEY)  );

CREATE   TABLE partsupp ( PS_PARTKEY     INTEGER NOT NULL,
                             PS_SUPPKEY     INTEGER NOT NULL,
                             PS_AVAILQTY    INTEGER NOT NULL,
                             PS_SUPPLYCOST  numeric(15,2)  NOT NULL,
                             PS_COMMENT     VARCHAR(199) NOT NULL,
                             PRIMARY KEY (PS_PARTKEY, PS_SUPPKEY),
                             FOREIGN KEY (PS_SUPPKEY) REFERENCES SUPPLIER (S_SUPPKEY) ,
                             FOREIGN KEY (PS_PARTKEY) REFERENCES PART (P_PARTKEY) 
                             );

CREATE   TABLE customer ( C_CUSTKEY     INTEGER NOT NULL,
                             C_NAME        VARCHAR(25) NOT NULL,
                             C_ADDRESS     VARCHAR(40) NOT NULL,
                             C_NATIONKEY   INTEGER NOT NULL,
                             C_PHONE       CHAR(15) NOT NULL,
                             C_ACCTBAL     numeric(15,2)   NOT NULL,
                             C_MKTSEGMENT  CHAR(10) NOT NULL,
                             C_COMMENT     VARCHAR(117) NOT NULL,
                             PRIMARY KEY (C_CUSTKEY),
                             FOREIGN KEY (C_NATIONKEY) REFERENCES NATION (N_NATIONKEY) );

CREATE   TABLE orders ( O_ORDERKEY       INTEGER NOT NULL,
                           O_CUSTKEY        INTEGER NOT NULL,
                           O_ORDERSTATUS    CHAR(1) NOT NULL,
                           O_TOTALPRICE     numeric(15,2) NOT NULL,
                           O_ORDERDATE      DATE NOT NULL,
                           O_ORDERPRIORITY  CHAR(15) NOT NULL,  
                           O_CLERK          CHAR(15) NOT NULL, 
                           O_SHIPPRIORITY   INTEGER NOT NULL,
                           O_COMMENT        VARCHAR(79) NOT NULL,
                           PRIMARY KEY (O_ORDERKEY),
                           FOREIGN KEY (O_CUSTKEY) REFERENCES CUSTOMER (C_CUSTKEY) );

CREATE   TABLE lineitem ( L_ORDERKEY    INTEGER NOT NULL,
                             L_PARTKEY     INTEGER NOT NULL,
                             L_SUPPKEY     INTEGER NOT NULL,
                             L_LINENUMBER  INTEGER NOT NULL,
                             L_QUANTITY    numeric(15,2) NOT NULL,
                             L_EXTENDEDPRICE  numeric(15,2) NOT NULL,
                             L_DISCOUNT    numeric(15,2) NOT NULL,
                             L_TAX         numeric(15,2) NOT NULL,
                             L_RETURNFLAG  CHAR(1) NOT NULL,
                             L_LINESTATUS  CHAR(1) NOT NULL,
                             L_SHIPDATE    DATE NOT NULL,
                             L_COMMITDATE  DATE NOT NULL,
                             L_RECEIPTDATE DATE NOT NULL,
                             L_SHIPINSTRUCT CHAR(25) NOT NULL,
                             L_SHIPMODE     CHAR(10) NOT NULL,
                             L_COMMENT      VARCHAR(44) NOT NULL,
                             PRIMARY KEY (L_ORDERKEY, L_LINENUMBER),
                             FOREIGN KEY (L_ORDERKEY) REFERENCES ORDERS (O_ORDERKEY) ,
                           FOREIGN KEY (L_SUPPKEY, L_PARTKEY) REFERENCES PARTSUPP (PS_SUPPKEY,PS_PARTKEY) );
