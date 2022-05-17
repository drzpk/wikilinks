-- MySQL dump 10.19  Distrib 10.3.34-MariaDB, for debian-linux-gnu (x86_64)
--
-- Table structure for table `pagelinks`
--

DROP TABLE IF EXISTS `pagelinks`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `pagelinks` (
                             `pl_from` int(8) unsigned NOT NULL DEFAULT 0,
                             `pl_namespace` int(11) NOT NULL DEFAULT 0,
                             `pl_title` varbinary(255) NOT NULL DEFAULT '',
                             `pl_from_namespace` int(11) NOT NULL DEFAULT 0,
                             PRIMARY KEY (`pl_from`,`pl_namespace`,`pl_title`),
                             KEY `pl_namespace` (`pl_namespace`,`pl_title`,`pl_from`),
                             KEY `pl_backlinks_namespace` (`pl_from_namespace`,`pl_namespace`,`pl_title`,`pl_from`)
) ENGINE=InnoDB DEFAULT CHARSET=binary ROW_FORMAT=COMPRESSED;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `pagelinks`
--

/*!40000 ALTER TABLE `pagelinks` DISABLE KEYS */;
INSERT INTO `pagelinks` VALUES (586,0,'!',0),(4748,0,'!',0),(9773,0,'!',0);

INSERT INTO `something_else` VALUES (1),(2),(3);


