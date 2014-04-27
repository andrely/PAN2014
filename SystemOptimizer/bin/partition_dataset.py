from glob import glob
import logging
from optparse import OptionParser
import os
from random import shuffle
import sys

sys.path.append(os.path.join(os.path.dirname(__file__), '..'))

from system_optimizer.extract_plagiarism import parse_subcorpora_dir


if __name__ == '__main__':
    logging.basicConfig(level=logging.INFO)

    parser = OptionParser()
    parser.add_option('-c', '--corpus-path', default=os.getcwd())
    parser.add_option('-d', '--dataset-file')
    opts, args = parser.parse_args()

    corpus_path = opts.corpus_path
    logging.info("Reading corpus from %s" % corpus_path)

    dataset_fn = opts.dataset_file

    if not dataset_fn:
        print parser.get_usage()
        exit(1)

    with open(dataset_fn) as f:
        header = f.readline().strip()

        score_lines = [line.strip().split('\t') for line in f]

    no_plag_scores = [m for m in score_lines if m[2] == 'no-plagiarism']
    no_plag_idx = 0

    for dir in glob(os.path.join(corpus_path, '*')):
        if not os.path.isdir(dir):
            continue

        subcorpora = parse_subcorpora_dir(os.path.basename(os.path.normpath(dir)))

        if not subcorpora:
            continue

        pairs_fn = os.path.join(dir, 'pairs')

        with open(pairs_fn) as f:
            pairs = [line.strip().split() for line in f]

        n = len(pairs)

        shuffle(pairs)

        train_len = int(0.8*n)
        held_len = int(0.1*n)
        test_len = n - train_len - held_len

        train_pairs = pairs[0:train_len]
        held_pairs = pairs[train_len:train_len+held_len]
        test_pairs = pairs[train_len+held_len:]

        with open('pairs-%s-train' % subcorpora, 'w') as f:
            for p in train_pairs:
                f.write("%s %s\n" % (p[0], p[1]))

        with open('pairs-%s-held' % subcorpora, 'w') as f:
            for p in held_pairs:
                f.write("%s %s\n" % (p[0], p[1]))

        with open('pairs-%s-test' % subcorpora, 'w') as f:
            for p in test_pairs:
                f.write("%s %s\n" % (p[0], p[1]))

        with open('%s-train.tsv' % subcorpora, 'w') as f:
            f.write(header + '\n')

            train_count = 0

            for score in score_lines:
                susp_id = '-'.join(score[0].split('-')[:-1])
                src_id = '-'.join(score[1].split('-')[:-1])

                if [susp_id, src_id] in train_pairs:
                    train_count += 1
                    f.write('\t'.join(score) + '\n')

            for score in no_plag_scores[no_plag_idx:no_plag_idx+train_count]:
                f.write('\t'.join(score) + '\n')

            no_plag_idx += train_count

        with open('%s-held.tsv' % subcorpora, 'w') as f:
            f.write(header + '\n')

            held_count = 0

            for score in score_lines:
                susp_id = '-'.join(score[0].split('-')[:-1])
                src_id = '-'.join(score[1].split('-')[:-1])

                if [susp_id, src_id] in held_pairs:
                    held_count += 1
                    f.write('\t'.join(score) + '\n')

            for score in no_plag_scores[no_plag_idx:no_plag_idx+held_count]:
                f.write('\t'.join(score) + '\n')

            no_plag_idx += held_count

        with open('%s-test.tsv' % subcorpora, 'w') as f:
            f.write(header + '\n')

            test_count = 0

            for score in score_lines:
                susp_id = '-'.join(score[0].split('-')[:-1])
                src_id = '-'.join(score[1].split('-')[:-1])

                if [susp_id, src_id] in test_pairs:
                    test_count += 1
                    f.write('\t'.join(score) + '\n')

            for score in no_plag_scores[no_plag_idx:no_plag_idx+test_count]:
                f.write('\t'.join(score) + '\n')

            no_plag_idx += test_count
