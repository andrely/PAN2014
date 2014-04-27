import logging
import sys

if __name__ == '__main__':
    logging.basicConfig(level=logging.INFO)

    if len(sys.argv) < 3:
        print "Usage: combine_scores.py <matched sentences file> <score file> ..."

        sys.exit(1)

    matched_sent_fn = sys.argv[1]
    score_fns = sys.argv[2:]

    matched_sent_file = open(matched_sent_fn)

    matched_sent_lines = matched_sent_file.readlines()
    matched_sent_file.close()

    score_files = [open(fn) for fn in score_fns]
    score_headers = [file.readline().split('\t') for file in score_files]
    score_lines = zip(*[file.readlines() for file in score_files])
    [file.close() for file in score_files]

    if len(matched_sent_lines) != len(score_lines):
        logging.warn("input file length does not match")
        sys.exit(1)

    sys.stdout.write("suspid\tsrcid\tplagtype\t" + '\t'.join(['\t'.join(h[0:-1]) for h in score_headers]) + '\n')

    for match_line, score_lines in zip(matched_sent_lines, score_lines):
        sys.stdout.write(match_line.strip() + '\t' +
                         '\t'.join(['\t'.join(line.split('\t')[0:-1]) for line in score_lines]) + '\n')
