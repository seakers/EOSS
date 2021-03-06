clear all;
clc;
r = global_jess_engine;
jess defmodule CAPABILITIES;
jess defmodule REQUIREMENTS;
jess defmodule AGGREGATION;

jess deftemplate CAPABILITIES::measurement (slot id) (slot taken-by) (slot parameter) (slot hsr#) (slot tr#) (slot all-weather) (slot acc#);
jess deftemplate CAPABILITIES::attribute (slot id) (slot parameter) (slot parent) (slot name) (slot type) (multislot fuzzy-value);
jess deftemplate REQUIREMENTS::attribute (slot id) (slot parameter) (slot parent) (slot name) (slot type) (slot nlevels) (multislot thresholds) (multislot scores) (multislot fuzzy-value);
jess deftemplate REQUIREMENTS::subobjective (slot id) (slot parent) (slot parameter) (multislot children) (slot aggreg-oper) (multislot agg-op-params) (slot satisfaction) (multislot fuzzy-value);
jess deftemplate AGGREGATION::objective (slot id) (slot parent) (multislot children) (slot satisfaction) (slot aggreg-oper) (multislot agg-op-params) (multislot fuzzy-value);
jess deftemplate AGGREGATION::stakeholder (slot id) (multislot children) (slot satisfaction) (slot aggreg-oper) (multislot agg-op-params) (multislot fuzzy-value);
jess deftemplate AGGREGATION::value (multislot children) (slot satisfaction) (slot aggreg-oper) (multislot agg-op-params) (multislot fuzzy-value);

jess deffacts AGGREGATION::stakeholders ...
    (AGGREGATION::value (children (create$ CL WE)) (aggreg-oper weighted-average) (agg-op-params (create$ 0.5 0.5))) ...
    (AGGREGATION::stakeholder (id CL) (children (create$ CL1 CL2)) (aggreg-oper weighted-average) (agg-op-params (create$ 0.5 0.5))) ...
    (AGGREGATION::stakeholder (id WE) (children (create$ WE1 WE2 WE3)) (aggreg-oper weighted-average) (agg-op-params (create$ 0.33 0.34 0.33)));

jess deffacts AGGREGATION::objectives ...
    (AGGREGATION::objective (id CL1) (parent CL) (children (create$ CL1-1 CL1-2)) (aggreg-oper weighted-average) (agg-op-params (create$ 0.5 0.5))) ...
    (AGGREGATION::objective (id CL2) (parent CL) (children (create$ CL2-1 CL2-2)) (aggreg-oper weighted-average) (agg-op-params (create$ 0.5 0.5))) ...
    (AGGREGATION::objective (id WE1) (parent WE) (children (create$ WE1-1 WE1-2 WE1-3)) (aggreg-oper weighted-average) (agg-op-params (create$ 0.5 0.3 0.2))) ...
    (AGGREGATION::objective (id WE2) (parent WE) (children (create$ WE2-1 WE2-2)) (aggreg-oper weighted-average) (agg-op-params (create$ 0.5 0.5))) ...
    (AGGREGATION::objective (id WE3) (parent WE) (children (create$ WE3-1)) (aggreg-oper weighted-average) (agg-op-params (create$ 1.0)));

jess deffacts AGGREGATION::subobjectives ...
    (REQUIREMENTS::subobjective (id CL1-1) (parent CL1) (parameter "soil moisture") (children (create$ CL1-1-HSR# CL1-1-acc#)) (aggreg-oper prod)) ...
    (REQUIREMENTS::subobjective (id CL1-2) (parent CL1) (parameter "atmospheric temperature") (children (create$ CL1-2-HSR# CL1-2-TR#))(aggreg-oper prod)) ...
    (REQUIREMENTS::subobjective (id CL2-1) (parent CL2) (parameter "atmospheric humidity") (children (create$ CL2-1-HSR# CL2-1-TR#))(aggreg-oper prod)) ...
    (REQUIREMENTS::subobjective (id CL2-2) (parent CL2) (parameter "atmospheric winds") (children (create$ CL2-2-HSR# CL2-2-TR#))(aggreg-oper prod)) ...
    (REQUIREMENTS::subobjective (id WE1-1) (parent WE1) (parameter "soil moisture") (children (create$ WE1-1-HSR# WE1-1-acc#))(aggreg-oper prod)) ...
    (REQUIREMENTS::subobjective (id WE1-2) (parent WE1) (parameter "atmospheric temperature") (children (create$ WE1-2-HSR# WE1-2-TR#))(aggreg-oper prod)) ...
    (REQUIREMENTS::subobjective (id WE1-3) (parent WE1) (parameter "ocean altimetry") (children (create$ WE1-3-HSR# WE1-3-TR#))(aggreg-oper prod)) ...
    (REQUIREMENTS::subobjective (id WE2-1) (parent WE2) (parameter "atmospheric humidity") (children (create$ WE2-1-HSR# WE2-1-TR#))(aggreg-oper prod)) ...
    (REQUIREMENTS::subobjective (id WE2-2) (parent WE2) (parameter "atmospheric winds") (children (create$ WE2-2-HSR# WE2-2-TR#))(aggreg-oper prod)) ...
    (REQUIREMENTS::subobjective (id WE3-1) (parent WE3) (parameter "land temperature") (children (create$ WE3-1-HSR# WE3-1-TR#))(aggreg-oper prod)) ...

jess deffacts AGGREGATION::attributes ...
    (REQUIREMENTS::attribute (id CL1-1-HSR#) (parent CL1-1) (name HSR#) (parameter "soil moisture") (type SIB) (nlevels 3) (thresholds (create$ 4000 10000)) (scores (create$ 1.0 0.5 0.0))) ...
    (REQUIREMENTS::attribute (id CL1-1-acc#) (parent CL1-1) (name acc#) (parameter "soil moisture") (type SIB) (nlevels 3) (thresholds (create$ 0.04 0.10)) (scores (create$ 1.0 0.5 0.0))) ...
    (REQUIREMENTS::attribute (id CL1-2-HSR#) (parent CL1-2) (name HSR#) (parameter "atmospheric temperature") (type SIB) (nlevels 3) (thresholds (create$ 4000 10000)) (scores (create$ 1.0 0.5 0.0))) ...
    (REQUIREMENTS::attribute (id CL1-2-TR#) (parent CL1-2) (name TR#) (parameter "atmospheric temperature") (type SIB) (nlevels 3) (thresholds (create$ 24 72)) (scores (create$ 1.0 0.5 0.0))) ...
    (REQUIREMENTS::attribute (id CL2-1-HSR#) (parent CL2-1) (name HSR#) (parameter "atmospheric humidity") (type SIB) (nlevels 3) (thresholds (create$ 4000 10000)) (scores (create$ 1.0 0.5 0.0))) ...
    (REQUIREMENTS::attribute (id CL2-1-TR#) (parent CL2-1) (name TR#) (parameter "atmospheric humidity") (type SIB) (nlevels 3) (thresholds (create$ 24 72)) (scores (create$ 1.0 0.5 0.0))) ...
    (REQUIREMENTS::attribute (id CL2-2-HSR#) (parent CL2-2) (name HSR#) (parameter "atmospheric winds") (type SIB) (nlevels 3) (thresholds (create$ 4000 10000)) (scores (create$ 1.0 0.5 0.0))) ...
    (REQUIREMENTS::attribute (id CL2-2-TR#) (parent CL2-2) (name TR#) (parameter "atmospheric winds") (type SIB) (nlevels 3) (thresholds (create$ 24 72)) (scores (create$ 1.0 0.5 0.0))) ...
    (REQUIREMENTS::attribute (id WE1-1-HSR#) (parent WE1-1) (name HSR#) (parameter "soil moisture") (type SIB) (nlevels 3) (thresholds (create$ 4000 10000)) (scores (create$ 1.0 0.5 0.0))) ...
    (REQUIREMENTS::attribute (id WE1-1-acc#) (parent WE1-1) (name acc#) (parameter "soil moisture") (type SIB) (nlevels 3) (thresholds (create$ 0.04 0.10)) (scores (create$ 1.0 0.5 0.0))) ...
    (REQUIREMENTS::attribute (id WE1-2-HSR#) (parent WE1-2) (name HSR#) (parameter "atmospheric temperature") (type SIB) (nlevels 3) (thresholds (create$ 4000 10000)) (scores (create$ 1.0 0.5 0.0))) ...
    (REQUIREMENTS::attribute (id WE1-2-TR#) (parent WE1-2) (name TR#) (parameter "atmospheric temperature") (type SIB) (nlevels 3) (thresholds (create$ 24 72)) (scores (create$ 1.0 0.5 0.0))) ...
    (REQUIREMENTS::attribute (id WE1-3-HSR#) (parent WE1-3) (name HSR#) (parameter "ocean altimetry") (type SIB) (nlevels 3) (thresholds (create$ 4000 10000)) (scores (create$ 1.0 0.5 0.0))) ...
    (REQUIREMENTS::attribute (id WE1-3-TR#) (parent WE1-3) (name TR#) (parameter "ocean altimetry") (type SIB) (nlevels 3) (thresholds (create$ 24 72)) (scores (create$ 1.0 0.5 0.0))) ...
    (REQUIREMENTS::attribute (id WE2-1-HSR#) (parent WE2-1) (name HSR#) (parameter "atmospheric humidity") (type SIB) (nlevels 3) (thresholds (create$ 4000 10000)) (scores (create$ 1.0 0.5 0.0))) ...
    (REQUIREMENTS::attribute (id WE2-1-TR#) (parent WE2-1) (name TR#) (parameter "atmospheric humidity") (type SIB) (nlevels 3) (thresholds (create$ 24 72)) (scores (create$ 1.0 0.5 0.0))) ...
    (REQUIREMENTS::attribute (id WE2-2-HSR#) (parent WE2-2) (name HSR#) (parameter "atmospheric winds") (type SIB) (nlevels 3) (thresholds (create$ 4000 10000)) (scores (create$ 1.0 0.5 0.0))) ...
    (REQUIREMENTS::attribute (id WE2-2-TR#) (parent WE2-2) (name TR#) (parameter "atmospheric winds") (type SIB) (nlevels 3) (thresholds (create$ 24 72)) (scores (create$ 1.0 0.5 0.0))) ...
    (REQUIREMENTS::attribute (id WE3-1-HSR#) (parent WE3-1) (name HSR#) (parameter "land temperature") (type SIB) (nlevels 3) (thresholds (create$ 4000 10000)) (scores (create$ 1.0 0.5 0.0))) ...
    (REQUIREMENTS::attribute (id WE3-1-TR#) (parent WE3-1) (name TR#) (parameter "land temperature") (type SIB) (nlevels 3) (thresholds (create$ 24 72)) (scores (create$ 1.0 0.5 0.0))) ...
    
jess defrule REQUIREMENTS::subobjective-satisfaction
