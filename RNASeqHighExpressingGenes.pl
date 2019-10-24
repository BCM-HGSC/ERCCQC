#!/usr/bin/perl
# simple script to take the output of the RNASeqQC package and the ERCC-QC output and make a
# tsv file of key values for upload into Lims
use strict;
use Getopt::Long;
my $usage = "RnaSeqLimsData.pl
  -rnaseqc 	tab delimited metrics file to be parsed
   -cuff	genes.fpkm_tracking file to parse
   -bam 	bam file
";
my $file1;
my $file2;
my $bam;

# define which stats to include in the lims upload
&GetOptions(
			 'rnaseqc:s' => \$file1,
			 'cuff:s'    => \$file2,
			 'bam:s'     => \$bam,
);
die($usage) unless ( $file1 && $file2 && $bam );

# list of the tags we would like to keep and what to call them
# white space removed from keys
open( my $RNASEQC, $file1 ) or die("Cannot open file $file1 \n");
open( my $CUFF,    $file2 ) or die("Cannot open file $file2 \n");
my $hash;
parseMetricsFile($RNASEQC);

# parse the cufflinks file to pull out the high expressing genes
my $genes;
while (<$CUFF>) {
	chomp;
	my @array = split("\t");
	if ( $array[12] eq 'HIDATA' ) {
		$genes->{ $array[4] }->{loc} = $array[6];
	}
}
if ($genes) {
	print STDERR "Found " . scalar( keys %$genes ) . " high expressing genes:\n";
	foreach my $key ( keys %$genes ) {
		print STDERR "$key\t" . $genes->{$key}->{loc} . "\n";

		# figure out how many reads they encompass
		my $cmd =
		    "samtools view -b -h $bam "
		  . $genes->{$key}->{loc}
		  . " | samtools flagstat - ";
		open( STREAM, "$cmd | " ) or die("Cannot execute command $cmd\n");
		while (<STREAM>) {
			chomp;
			if ( $_ =~ /(\d+) in total/ ) {
				$genes->{$key}->{tot} = $1;
			}
			if ( $_ =~ /(\d+) duplicates/ ) {
				$genes->{$key}->{dups} = $1;
			}
		}
	}
} else {
	print  "Num_highly_expressed_genes	0\n";
	print "High_expressing_rate	n/a\n";
	print "Adjusted_dup_rate	n/a\n\t";
	exit;
}
# store the values in the hash

print "Num_highly_expressed_genes\t" . scalar(keys %$genes) ."\n";
my $total_HE_reads = 0 ;
my $total_HE_dups = 0;
foreach my $gene ( keys %$genes ) {
	print STDERR "$gene "
	  . $genes->{$gene}->{tot}
	  . " total reads and "
	  . $genes->{$gene}->{dups}
	  . " duplicates\n";
	  $total_HE_reads += $genes->{$gene}->{tot};
	  $total_HE_dups  += $genes->{$gene}->{dups};
}

# now recalculate based on the new numbers
my $adjusted_reads = $hash->{'Mapped'} - $total_HE_reads;
my $adjusted_dups =   ($hash->{'Duplication_Rate_of_Mapped'} * $hash->{'Mapped'}) - $total_HE_dups;
# add the new values to the hash
print "High_expressing_rate\t";
print ( $total_HE_reads / $hash->{'Mapped'});
print "\n";
print "Adjusted_dup_rate\t";
print ( $adjusted_dups /  $adjusted_reads);
print "\n";



exit;

sub parseMetricsFile {
	my ($fh) = @_;
	my @header;
	my @data;
	while (<$fh>) {
		chomp;
		my $line = $_;
		$line =~ s/ /_/g;

		# parse the header and then the data
		if ( scalar(@header) == 0 ) {
			@header = split( /\t/, $line );
		} else {
			@data = split( /\t/, $line );
		}
	}

	# parse the output and generate the LIMS command
	# if you have an empty value DONT enter the corresponding key
	# as there are no delimiters on the command line
	# so you will get key - (next)key rather than key - value
	for ( my $i = 0 ; $i <= $#header ; $i++ ) {
		#print $header[$i] . " " . $data[$i] ."\n";
		$hash->{ $header[$i] } = $data[$i];
	}
}
