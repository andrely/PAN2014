from glob import glob
import logging
from optparse import OptionParser
import os
from random import sample
import sys

from pymongo import MongoClient


sys.path.append(os.path.join(os.path.dirname(__file__), '..'))

from system_optimizer.extract_plagiarism import parse_truth_fn, get_plagiarism


if __name__ == '__main__':
    logging.basicConfig(level=logging.INFO)

    parser = OptionParser()
    parser.add_option('-c', '--corpus-path', default=os.getcwd())
    opts, args = parser.parse_args()

    corpus_path = opts.corpus_path
    logging.info("Reading corpus from %s" % corpus_path)

    plag_sent_count = 0

    with MongoClient() as conn:
        db = conn['PAN2014']

        for dir in glob(os.path.join(corpus_path, '*')):
            if not os.path.isdir(dir):
                continue

            for fn in glob(os.path.join(dir, '*.xml')):
                try:
                    susp_fn, src_fn = parse_truth_fn(fn)
                except ValueError:
                    logging.info("Encountered unknown file name %s" % fn)
                    continue

                plagiarisms = get_plagiarism(fn, db)

                if not plagiarisms:
                    continue

                for plag in plagiarisms:
                    susp_fn, src_fn, plag_sents = plag

                    for susp_id, src_id in plag_sents:
                        sys.stdout.write("%s\t%s\tT\n" % (susp_id, src_id))

                        plag_sent_count += 1

        logging.info("Found %d plagiarized sentence pairs" % plag_sent_count)

        src_coll = db['source_sentences']
        susp_coll = db['suspicious_sentences']

        src_idx = sorted(sample(xrange(src_coll.count()), plag_sent_count), reverse=True)
        src_sents = []

        for i, row in enumerate(src_coll.find({}, {'id': 1})):
            if i == src_idx[-1]:
                src_sents.append(row['id'])
                src_idx.pop()

                if len(src_idx) == 0:
                    break

        susp_idx = sorted(sample(xrange(susp_coll.count()), plag_sent_count), reverse=True)
        susp_sents = []

        for i, row in enumerate(susp_coll.find({}, {'id': 1})):
            if i == susp_idx[-1]:
                susp_sents.append(row['id'])
                susp_idx.pop()

                if len(susp_idx) == 0:
                    break

        for susp_id, src_id in zip(susp_sents, src_sents):
            sys.stdout.write("%s\t%s\tF\n" % (susp_id, src_id))
