--
-- PostgreSQL database dump
--

-- Dumped from database version 16.2
-- Dumped by pg_dump version 16.2

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: adminpack; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS adminpack WITH SCHEMA pg_catalog;


--
-- Name: EXTENSION adminpack; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION adminpack IS 'administrative functions for PostgreSQL';


SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: product_tb; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.product_tb (
    id integer NOT NULL,
    name character varying(100),
    unit_price double precision,
    stock_qty integer,
    import_date date DEFAULT CURRENT_DATE
);


ALTER TABLE public.product_tb OWNER TO postgres;

--
-- Name: product_tb_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.product_tb_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.product_tb_id_seq OWNER TO postgres;

--
-- Name: product_tb_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.product_tb_id_seq OWNED BY public.product_tb.id;


--
-- Name: save_back_up_list_tb; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.save_back_up_list_tb (
    id integer NOT NULL,
    list_of_table character varying(300)
);


ALTER TABLE public.save_back_up_list_tb OWNER TO postgres;

--
-- Name: save_back_up_list_tb_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.save_back_up_list_tb_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.save_back_up_list_tb_id_seq OWNER TO postgres;

--
-- Name: save_back_up_list_tb_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.save_back_up_list_tb_id_seq OWNED BY public.save_back_up_list_tb.id;


--
-- Name: save_page_tb; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.save_page_tb (
    key character varying(50) NOT NULL,
    value integer
);


ALTER TABLE public.save_page_tb OWNER TO postgres;

--
-- Name: product_tb id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.product_tb ALTER COLUMN id SET DEFAULT nextval('public.product_tb_id_seq'::regclass);


--
-- Name: save_back_up_list_tb id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.save_back_up_list_tb ALTER COLUMN id SET DEFAULT nextval('public.save_back_up_list_tb_id_seq'::regclass);


--
-- Data for Name: product_tb; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.product_tb (id, name, unit_price, stock_qty, import_date) FROM stdin;
2	Dog Lay	56	43	2024-03-08
3	Dog seth	153	12	2024-03-08
4	Dog Lay Lay	12	134	2024-03-08
5	Lay Dog Dog	123	123	2024-03-08
\.


--
-- Data for Name: save_back_up_list_tb; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.save_back_up_list_tb (id, list_of_table) FROM stdin;
\.


--
-- Data for Name: save_page_tb; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.save_page_tb (key, value) FROM stdin;
pageSize	3
\.


--
-- Name: product_tb_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.product_tb_id_seq', 5, true);


--
-- Name: save_back_up_list_tb_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.save_back_up_list_tb_id_seq', 1, false);


--
-- Name: product_tb product_tb_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.product_tb
    ADD CONSTRAINT product_tb_pkey PRIMARY KEY (id);


--
-- Name: save_back_up_list_tb save_back_up_list_tb_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.save_back_up_list_tb
    ADD CONSTRAINT save_back_up_list_tb_pkey PRIMARY KEY (id);


--
-- Name: save_page_tb save_page_tb_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.save_page_tb
    ADD CONSTRAINT save_page_tb_pkey PRIMARY KEY (key);


--
-- PostgreSQL database dump complete
--

