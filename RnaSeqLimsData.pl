#!/usr/bin/perl
# simple script to take the output of the RNASeqQC package and the ERCC-QC output and make a
# tsv file of key values for upload into Lims
use strict;
use Getopt::Long;
my $usage = "RnaSeqLimsData.pl
  -rnaseqc tab delimited metrics file to be parsed
  -erccqc  tab delimited metrics file to be parsed
  -erccst  tab delimited ERCC alignment stats to be parsed
  -cuff	   cufflinks log file to parse
";
my $file1;
my $file2;
my $file3;
my $file4;
# define which stats to include in the lims upload
my $hash = defineLines();

&GetOptions( 'rnaseqc:s' => \$file1,
			 'erccqc:s'  => \$file2, 
			 'erccst:s'  => \$file3,
			 'cuff:s'	 => \$file4
			 );
			 
die($usage) unless ( $file1 && $file2 );

# list of the tags we would like to keep and what to call them
# white space removed from keys
open( my $RNASEQC, $file1 ) or die("Cannot open file $file1 \n");
open( my $ERCQC,   $file2 ) or die("Cannot open file $file2 \n");
open( my $ERCST,   $file3 ) or die("Cannot open file $file3 \n");
open( my $CUFF ,   $file4 ) or die("Cannot open file $file4 \n");
parseMetricsFile($RNASEQC);
parseMetricsFile($ERCQC);
parseMetricsFile($ERCST);
parseCufflinks($CUFF);
# finally print out the links to the html files
my $path = "http://128.249.42.223";
my @dirs = split(/\//,$file1);
#drop the filename and final dir 
pop(@dirs);
pop(@dirs);
$path .= join("/",@dirs);
print "RNASeQC\t$path" . "/RNA-SeQC/index.html\n";
print "ERCCQC\t$path" . "/ERCC-QC/index.html\n";
exit;

sub parseMetricsFile
{
	my ($fh) = @_;
	my @header;
	my @data;
	while (<$fh>)
	{
		chomp;
		my $line = $_;
		$line =~ s/ /_/g;
		# parse the header and then the data
		if ( scalar(@header) == 0 )
		{
			@header = split( /\t/, $line );
		} else
		{
			@data = split( /\t/, $line );
		}
	}

	# parse the output and generate the LIMS command
	# if you have an empty value DONT enter the corresponding key
	# as there are no delimiters on the command line
	# so you will get key - (next)key rather than key - value
	for ( my $i = 0 ; $i <= $#header ; $i++ )
	{
		if (  $hash->{ $header[$i] }  ) {
			print $hash->{ $header[$i] } . "\t" . $data[$i] . "\n";
		}
	}
}

sub parseCufflinks {
	my ($fh) = @_;
	while (<$fh>){
		chomp;
		if ( $_ =~ />\s+Estimated Mean:\s+(\d+\.*\d*)$/){
			print "Fragment_Length_Mean\t$1\n";			
		}
		if ( $_ =~ />\s+Estimated Std Dev:\s+(\d+\.*\d*)$/){
			print "Fragment_Length_StdDev\t$1\n";			
		}
	}
	
}

sub defineLines
{

	# hash of the stats to include in the lims upload
	# key = as it appears in the output files
	# value = as it will appear in lims
	# 0 = don't use
	my $hash = {
	  "Sample" => "SampleName",
	  "Note" => undef,
	  "End_2_Mapping_Rate" => "End_2_Mapping_Rate",
	  "Chimeric_Pairs" => "Chimeric_Pairs",
	  "Intragenic_Rate" => "Intragenic_Rate",
	  "Num._Gaps" => "Num_Gaps",
	  "Mapping_Rate" => "Mapping_Rate",
	  "Exonic_Rate" => "Exonic_Rate",
	  "5'_Norm" => "5_prime_Norm",
	  "Genes_Detected" => "Genes_Detected",
	  "Unique_Rate_of_Mapped" => "Unique_Rate_of_Mapped",
	  "Read_Length" => "Read_Length",
	  "Mean_Per_Base_Cov." => "Mean_Per_Base_Cov",
	  "End_1_Mismatch_Rate" => "End_1_Mismatch_Rate",
	  "Estimated_Library_Size" => "Estimated_Library_Size",
	  "Mapped" => "Mapped",
	  "Intergenic_Rate" => "Intergenic_Rate",
	  "rRNA" => "rRNA",
	  "Total_Purity_Filtered_Reads_Sequenced" => "Total_Purity_Filtered_Reads_Sequenced",
	  "Failed_Vendor_QC_Check" => "Failed_Vendor_QC_Check",
	  "Mean_CV" => "Mean_CV",
	  "Transcripts_Detected" => "Transcripts_Detected",
	  "Mapped_Pairs" => "Mapped_Pairs",
	  "Cumul._Gap_Length" => "Cumul_Gap_Length",
	  "Gap_%" => "Percent_Gap",
	  "Unpaired_Reads" => "Unpaired_Reads",
	  "Intronic_Rate" => "Intronic_Rate",
	  "Mapped_Unique_Rate_of_Total" => "Mapped_Unique_Rate_of_Total",
	  "Expression_Profiling_Efficiency" => "Expression_Profiling_Efficiency",
	  "Mapped_Unique" => "Mapped_Unique",
	  "End_2_Mismatch_Rate" => "End_2_Mismatch_Rate",
	  "End_2_Antisense" => "End_2_Antisense",
	  "Alternative_Aligments" => "Alternative_Aligments",
	  "End_2_Sense" => "End_2_Sense",
	  "End_1_Antisense" => "End_1_Antisense",
	  "Base_Mismatch_Rate" => "Base_Mismatch_Rate",
	  "End_1_Sense" => "End_1_Sense",
	  "End_1_%_Sense" => "Percent_End_1_Sense",
	  "rRNA_rate" => "rRNA_rate",
	  "End_1_Mapping_Rate" => "End_1_Mapping_Rate",
	  "No._Covered_5'" => "No_Covered_5_prime",
	  "Duplication_Rate_of_Mapped" => "Duplication_Rate_of_Mapped",
	  "End_2_%_Sense" => "Percent_End_2_Sense",
	  "R^2" => "ERCC_R_Squared",
	  "Correlation" => "ERCC_Correlation",
	  "number_of_ERCC_transcripts_identified" => "Num_ERCC_Transcripts_Identified",
	  "Number_of_ERCC_Alignments" => "Num_ERCC_Alignments",
	  "Percent_ERCC_duplicated" => "ERCC_dup_rate",};
	return $hash;
}
