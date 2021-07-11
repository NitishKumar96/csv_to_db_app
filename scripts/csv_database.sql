-- phpMyAdmin SQL Dump
-- version 5.0.4
-- https://www.phpmyadmin.net/
--
-- Host: mydb:3306
-- Generation Time: Jul 11, 2021 at 01:59 PM
-- Server version: 8.0.25
-- PHP Version: 7.4.13

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `csv_database`
--

-- --------------------------------------------------------

--
-- Table structure for table `csv_analysis`
--

CREATE TABLE `csv_analysis` (
  `id` int NOT NULL,
  `table_name` varchar(100) NOT NULL,
  `col_name` varchar(100) DEFAULT NULL,
  `keyword` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `operation` varchar(50) NOT NULL,
  `value` int NOT NULL,
  `status` int NOT NULL DEFAULT '1',
  `created_on` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Table structure for table `master_csv`
--

CREATE TABLE `master_csv` (
  `id` int NOT NULL,
  `csv_name` varchar(100) NOT NULL,
  `table_name` varchar(100) NOT NULL,
  `status` int NOT NULL DEFAULT '1',
  `uploaded_on` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `lines_read` int NOT NULL DEFAULT '0',
  `lines_saved` int NOT NULL DEFAULT '0',
  `time_taken` bigint DEFAULT NULL,
  `num_thread` int DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Indexes for dumped tables
--

--
-- Indexes for table `csv_analysis`
--
ALTER TABLE `csv_analysis`
  ADD PRIMARY KEY (`id`);

--
-- Indexes for table `master_csv`
--
ALTER TABLE `master_csv`
  ADD PRIMARY KEY (`id`);

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `csv_analysis`
--
ALTER TABLE `csv_analysis`
  MODIFY `id` int NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `master_csv`
--
ALTER TABLE `master_csv`
  MODIFY `id` int NOT NULL AUTO_INCREMENT;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
