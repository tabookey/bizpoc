$data = `strings "$ARGV[0]"|grep Tj|sed -n '/your private/,/your backup/p'`;
$data =~ s/.*\((.*)\) Tj\s*/$1/g;

die "not bitgo pdf: $ARGV[0]\n" unless $data;
($json) = $data=~/(\{[\S]*\})/;


($keyid) = `strings "$ARGV[0]"` =~ /Key Id:\s*(.*)\)/;
print "$json\n";
print "$keyid\n";
